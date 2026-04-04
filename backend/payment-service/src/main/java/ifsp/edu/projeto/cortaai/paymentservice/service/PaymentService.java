package ifsp.edu.projeto.cortaai.paymentservice.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import ifsp.edu.projeto.cortaai.paymentservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.event.PaymentApprovedEvent;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.model.PaymentStatus;
import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import ifsp.edu.projeto.cortaai.paymentservice.model.WebhookLog;
import ifsp.edu.projeto.cortaai.paymentservice.repository.TransactionRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final ScheduleServiceClient scheduleServiceClient;
    private final UserServiceClient userServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    /**
     * Taxa da plataforma CortaAI cobrada sobre cada transação (5%).
     */
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");

    /**
     * Cria um pagamento (Checkout Pro do Mercado Pago) com suporte a Split.
     * 1. Busca agendamento via Feign
     * 2. Busca credenciais MP do barbeiro via Feign
     * 3. Cria preferência no MP com application_fee (split)
     * 4. Salva Transaction com status PENDING e método de pagamento
     *
     * @param paymentMethod "PIX" ou "CREDIT_CARD"
     */
    @Transactional
    public TransactionDTO createPayment(UUID appointmentId, UUID customerId, String paymentMethod) {
        // Verificar se já existe transação para este agendamento
        transactionRepository.findByAppointmentId(appointmentId).ifPresent(tx -> {
            if (tx.getStatus() == PaymentStatus.PENDING || tx.getStatus() == PaymentStatus.APPROVED) {
                throw new RuntimeException("Já existe um pagamento para este agendamento");
            }
        });

        // Buscar dados do agendamento via Feign
        AppointmentInfoDTO appointment = scheduleServiceClient.getAppointmentById(appointmentId);

        if (!appointment.customerId().equals(customerId)) {
            throw new RuntimeException("Este agendamento não pertence ao usuário");
        }

        // Calcular taxas para o split
        BigDecimal grossAmount = appointment.totalPrice();
        BigDecimal platformFee = grossAmount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        try {
            // Criar preferência no Mercado Pago
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Agendamento - " + appointment.barbershopName())
                    .description("Atendimento com " + appointment.barberName() + " em " + appointment.startTime())
                    .quantity(1)
                    .currencyId("BRL")
                    .unitPrice(grossAmount)
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success("https://cortaai.shop/payment/success")
                    .failure("https://cortaai.shop/payment/failure")
                    .pending("https://cortaai.shop/payment/pending")
                    .build();

            PreferenceRequest.PreferenceRequestBuilder preferenceBuilder = PreferenceRequest.builder()
                    .items(List.of(itemRequest))
                    .backUrls(backUrls)
                    .notificationUrl(notificationUrl)
                    .externalReference(appointmentId.toString())
                    .autoReturn("approved");

            // NOTA: application_fee (split de plataforma) será adicionado após
            // implementação completa do OAuth do barbeiro (MP Marketplace).
            // Por enquanto, o pagamento vai integralmente para a conta da plataforma.

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceBuilder.build());

            // Salvar transação
            Transaction transaction = Transaction.builder()
                    .appointmentId(appointmentId)
                    .customerId(customerId)
                    .amount(grossAmount)
                    .grossAmount(grossAmount)
                    .platformFeeAmount(platformFee)
                    .paymentMethod(paymentMethod != null ? paymentMethod : "CREDIT_CARD")
                    .status(PaymentStatus.PENDING)
                    .mpPreferenceId(preference.getId())
                    .checkoutUrl(preference.getInitPoint())
                    .build();

            Transaction saved = transactionRepository.save(transaction);
            log.info("Pagamento criado: txId={}, preferenceId={}, method={}", saved.getId(), preference.getId(), paymentMethod);

            return toDTO(saved);

        } catch (Exception e) {
            log.error("Erro ao criar preferência no Mercado Pago: {}", e.getMessage());
            throw new RuntimeException("Falha ao criar pagamento: " + e.getMessage());
        }
    }

    /**
     * Mantém compatibilidade retroativa — sem método de pagamento explícito.
     */
    @Transactional
    public TransactionDTO createPayment(UUID appointmentId, UUID customerId) {
        return createPayment(appointmentId, customerId, "CREDIT_CARD");
    }

    /**
     * Processa webhook do Mercado Pago.
     * Idempotente — verifica WebhookLog antes de processar.
     */
    @Transactional
    public void processWebhook(String resourceId, String eventType, String rawPayload) {
        // Idempotência: verificar se já processamos
        if (webhookLogRepository.existsByMpResourceIdAndProcessedTrue(resourceId)) {
            log.info("Webhook já processado: resourceId={}", resourceId);
            return;
        }

        // Salvar log do webhook
        WebhookLog webhookLog = WebhookLog.builder()
                .mpResourceId(resourceId)
                .eventType(eventType)
                .rawPayload(rawPayload)
                .processed(false)
                .build();
        webhookLogRepository.save(webhookLog);

        if (!"payment".equals(eventType)) {
            log.info("Webhook ignorado: eventType={}", eventType);
            webhookLog.setProcessed(true);
            webhookLogRepository.save(webhookLog);
            return;
        }

        try {
            // Consultar pagamento no Mercado Pago
            com.mercadopago.client.payment.PaymentClient paymentClient =
                    new com.mercadopago.client.payment.PaymentClient();
            com.mercadopago.resources.payment.Payment mpPayment =
                    paymentClient.get(Long.parseLong(resourceId));

            String externalReference = mpPayment.getExternalReference();
            String mpStatus = mpPayment.getStatus();

            // Buscar transação pelo appointmentId (external reference)
            UUID appointmentId = UUID.fromString(externalReference);
            Transaction transaction = transactionRepository.findByAppointmentId(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Transação não encontrada para appointment: " + appointmentId));

            transaction.setMpPaymentId(resourceId);

            // Mapear status do MP para nosso enum
            PaymentStatus newStatus = mapMpStatus(mpStatus);
            transaction.setStatus(newStatus);
            transactionRepository.save(transaction);

            // Se aprovado: atualizar schedule + publicar evento
            if (newStatus == PaymentStatus.APPROVED) {
                // Atualizar status no schedule-service via Feign
                try {
                    scheduleServiceClient.updatePaymentStatus(appointmentId, "PAID");
                } catch (Exception e) {
                    log.error("Falha ao atualizar payment status no schedule-service: {}", e.getMessage());
                }

                // Publicar evento para notification-service
                String customerEmail = null;
                try {
                    var customerInfo = userServiceClient.getUserById(transaction.getCustomerId());
                    if (customerInfo != null) customerEmail = customerInfo.getEmail();
                } catch (Exception e) {
                    log.warn("Não foi possível buscar email do customer {}: {}", transaction.getCustomerId(), e.getMessage());
                }

                PaymentApprovedEvent event = new PaymentApprovedEvent(
                        transaction.getId(),
                        appointmentId,
                        transaction.getCustomerId(),
                        customerEmail,
                        transaction.getAmount()
                );
                rabbitTemplate.convertAndSend(
                        RabbitConfig.EXCHANGE,
                        RabbitConfig.RK_PAYMENT_APPROVED,
                        event
                );
                log.info("Evento PaymentApproved publicado: txId={}", transaction.getId());
            }

            webhookLog.setProcessed(true);
            webhookLogRepository.save(webhookLog);

            log.info("Webhook processado: resourceId={}, status={}", resourceId, newStatus);

        } catch (Exception e) {
            log.error("Erro ao processar webhook: resourceId={}, error={}", resourceId, e.getMessage());
            throw new RuntimeException("Falha ao processar webhook: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public TransactionDTO getById(UUID id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transação não encontrada: " + id));
        return toDTO(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionDTO> getMyPayments(UUID customerId) {
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private PaymentStatus mapMpStatus(String mpStatus) {
        return switch (mpStatus) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled" -> PaymentStatus.CANCELLED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "in_process", "pending", "authorized" -> PaymentStatus.IN_PROCESS;
            default -> PaymentStatus.PENDING;
        };
    }

    private TransactionDTO toDTO(Transaction tx) {
        return new TransactionDTO(
                tx.getId(),
                tx.getAppointmentId(),
                tx.getCustomerId(),
                tx.getAmount(),
                tx.getGrossAmount(),
                tx.getNetAmount(),
                tx.getMpFeeAmount(),
                tx.getPlatformFeeAmount(),
                tx.getPaymentMethod(),
                tx.getStatus(),
                tx.getCheckoutUrl(),
                tx.getCreatedAt(),
                tx.getUpdatedAt()
        );
    }
}
