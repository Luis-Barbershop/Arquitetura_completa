package ifsp.edu.projeto.cortaai.paymentservice.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import ifsp.edu.projeto.cortaai.paymentservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesPointDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.event.PaymentApprovedEvent;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ProductServiceClient;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final ScheduleServiceClient scheduleServiceClient;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
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
                    .barbershopId(appointment.barbershopId())
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
                    scheduleServiceClient.updatePaymentStatus(appointmentId, "CONFIRMED");
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

    @Transactional(readOnly = true)
    public List<TransactionDTO> getMyPaymentsByFirebaseUid(String firebaseUid) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return getMyPayments(user.getId());
    }

    @Transactional
    public TransactionDTO createPaymentByFirebaseUid(UUID appointmentId, String firebaseUid, String paymentMethod) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Usuário não encontrado para Firebase UID: " + firebaseUid);
        }
        return createPayment(appointmentId, user.getId(), paymentMethod);
    }

    @Transactional(readOnly = true)
    public FinancialOverviewDTO getBarbershopOverview(UUID barbershopId, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : startDate;
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<Transaction> periodTransactions = transactionRepository.findByBarbershopIdAndCreatedAtBetween(
                barbershopId, start, end);

        int approvedCount = (int) periodTransactions.stream().filter(t -> t.getStatus() == PaymentStatus.APPROVED).count();
        int pendingCount = (int) periodTransactions.stream().filter(t -> t.getStatus() == PaymentStatus.PENDING || t.getStatus() == PaymentStatus.IN_PROCESS).count();
        int cancelledCount = (int) periodTransactions.stream().filter(t -> t.getStatus() == PaymentStatus.CANCELLED).count();

        BigDecimal serviceRevenue = transactionRepository.sumAmountByBarbershopAndStatusAndCreatedAtBetween(
                barbershopId, PaymentStatus.APPROVED, start, end);

        InventoryFinancialSummaryDTO inventorySummary;
        try {
            inventorySummary = productServiceClient.getFinancialSummary(barbershopId, startDate, endDate);
        } catch (Exception ex) {
            log.warn("Falha ao buscar resumo financeiro de estoque para a barbearia {}: {}", barbershopId, ex.getMessage());
            inventorySummary = new InventoryFinancialSummaryDTO(barbershopId, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal productExpenses = inventorySummary.productExpenses() != null ? inventorySummary.productExpenses() : BigDecimal.ZERO;
        BigDecimal inventoryAssetValue = inventorySummary.inventoryAssetValue() != null ? inventorySummary.inventoryAssetValue() : BigDecimal.ZERO;
        BigDecimal operationalResult = serviceRevenue.subtract(productExpenses);

        return new FinancialOverviewDTO(
                barbershopId,
                "BRL",
                serviceRevenue,
                productExpenses,
                inventoryAssetValue,
                operationalResult,
                periodTransactions.size(),
                approvedCount,
                pendingCount,
                cancelledCount
        );
    }

    @Transactional(readOnly = true)
    public FinancialSeriesDTO getBarbershopSeries(UUID barbershopId, LocalDate from, LocalDate to, String groupBy) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(6);
        LocalDate endDate = to != null ? to : LocalDate.now();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        String safeGroupBy = groupBy == null ? "DAY" : groupBy.toUpperCase(Locale.ROOT);
        List<Transaction> periodTransactions = transactionRepository.findByBarbershopIdAndCreatedAtBetween(barbershopId, start, end)
                .stream()
                .filter(t -> t.getStatus() == PaymentStatus.APPROVED)
                .toList();

        Stream<Map.Entry<LocalDate, List<Transaction>>> groupedStream;
        if ("WEEK".equals(safeGroupBy)) {
            groupedStream = periodTransactions.stream()
                    .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate().with(java.time.DayOfWeek.MONDAY)))
                    .entrySet()
                    .stream();
        } else {
            groupedStream = periodTransactions.stream()
                    .collect(Collectors.groupingBy(t -> t.getCreatedAt().toLocalDate()))
                    .entrySet()
                    .stream();
            safeGroupBy = "DAY";
        }

        List<FinancialSeriesPointDTO> points = groupedStream
                .map(entry -> new FinancialSeriesPointDTO(
                        entry.getKey(),
                        entry.getValue().stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()
                ))
                .sorted(Comparator.comparing(FinancialSeriesPointDTO::date))
                .toList();

        return new FinancialSeriesDTO(barbershopId, safeGroupBy, points);
    }

    @Transactional(readOnly = true)
    public boolean canAccessBarbershopFinancials(UUID userId, UUID barbershopId, boolean ownerOnly) {
        UserInfoDTO user = userServiceClient.getUserById(userId);
        return checkBarbershopAccess(user, barbershopId, ownerOnly);
    }

    @Transactional(readOnly = true)
    public boolean canAccessBarbershopFinancials(String firebaseUid, UUID barbershopId, boolean ownerOnly) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        return checkBarbershopAccess(user, barbershopId, ownerOnly);
    }

    private boolean checkBarbershopAccess(UserInfoDTO user, UUID barbershopId, boolean ownerOnly) {
        if (user == null || user.getUserType() == null) {
            return false;
        }

        String userType = user.getUserType().toUpperCase(Locale.ROOT);
        if (!"BARBER".equals(userType)) {
            return false;
        }

        if (ownerOnly) {
            String role = user.getRole() != null ? user.getRole().toUpperCase(Locale.ROOT) : "";
            if (!role.contains("OWNER")) {
                return false;
            }
        }

        return barbershopId.equals(user.getBarbershopId());
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
                tx.getBarbershopId(),
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
