package ifsp.edu.projeto.cortaai.paymentservice.service;

import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import ifsp.edu.projeto.cortaai.paymentservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesPointDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.MpConnectionStatusDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.event.PaymentApprovedEvent;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.model.DashboardKpiDaily;
import ifsp.edu.projeto.cortaai.paymentservice.model.PaymentStatus;
import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import ifsp.edu.projeto.cortaai.paymentservice.model.WebhookLog;
import ifsp.edu.projeto.cortaai.paymentservice.repository.DashboardKpiDailyRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.TransactionRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final DashboardKpiDailyRepository dashboardKpiDailyRepository;
    private final ScheduleServiceClient scheduleServiceClient;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${mercadopago.webhook.secret:}")
    private String webhookSecret;

    @Value("${mercadopago.webhook.replay-window-seconds:300}")
    private long webhookReplayWindowSeconds;

    /**
     * Taxa da plataforma CortaAI cobrada sobre cada transação (5%).
     */
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.05");
    private static final UUID WALK_IN_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
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
                    .autoReturn("approved")
                    // "NONE" desabilita o modo marketplace para esta preferência, permitindo
                    // usar o token da plataforma. Quando o split estiver implementado via
                    // MPRequestOptions com o token OAuth do barbeiro, remover esta linha.
                    .marketplace("NONE");

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
        log.info("event=payment-created txId={} preferenceId={} method={}",
            maskIdentifier(saved.getId()),
            maskIdentifier(preference.getId()),
            paymentMethod);

            return toDTO(saved);

        } catch (Exception e) {
            log.error("event=payment-create-failed cause={}", e.getClass().getSimpleName(), e);
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
            log.info("event=webhook-already-processed resourceId={}", maskIdentifier(resourceId));
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
            log.info("event=webhook-ignored eventType={}", eventType);
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
            PaymentStatus previousStatus = transaction.getStatus();

            // Mapear status do MP para nosso enum
            PaymentStatus newStatus = mapMpStatus(mpStatus);
            transaction.setStatus(newStatus);
            transactionRepository.save(transaction);

            // Se aprovado: atualizar schedule + publicar evento
            if (newStatus == PaymentStatus.APPROVED && previousStatus != PaymentStatus.APPROVED) {
                updateDailyKpiProjectionForApproved(transaction);

                // Atualizar status no schedule-service via Feign
                try {
                    scheduleServiceClient.updatePaymentStatus(appointmentId, "CONFIRMED");
                } catch (Exception e) {
                    log.error("Falha ao atualizar payment status no schedule-service", e);
                }

                // Publicar evento para notification-service
                String customerEmail = null;
                try {
                    var customerInfo = userServiceClient.getUserById(transaction.getCustomerId());
                    if (customerInfo != null) customerEmail = customerInfo.getEmail();
                } catch (Exception e) {
                    log.warn("event=customer-email-fetch-failed customerId={}", maskIdentifier(transaction.getCustomerId()));
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
                log.info("event=payment-approved-published txId={}", maskIdentifier(transaction.getId()));
            }

            webhookLog.setProcessed(true);
            webhookLogRepository.save(webhookLog);

            log.info("event=webhook-processed resourceId={} status={}", maskIdentifier(resourceId), newStatus);

        } catch (Exception e) {
            log.error("event=webhook-process-failed resourceId={} cause={}",
                    maskIdentifier(resourceId),
                    e.getClass().getSimpleName(),
                    e);
            throw new RuntimeException("Falha ao processar webhook: " + e.getMessage());
        }
    }

    /**
     * Valida assinatura do webhook Mercado Pago quando segredo está configurado.
     * Se segredo não estiver configurado, mantém compatibilidade aceitando o webhook.
     */
    public boolean isWebhookTrusted(String resourceId, String xSignature, String xRequestId) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true;
        }

        if (isBlank(resourceId) || isBlank(xSignature) || isBlank(xRequestId)) {
            log.warn("Webhook sem cabecalhos obrigatorios para validacao de assinatura");
            return false;
        }

        Map<String, String> signatureParts = parseSignatureHeader(xSignature);
        String tsRaw = signatureParts.get("ts");
        String v1 = signatureParts.get("v1");

        if (isBlank(tsRaw) || isBlank(v1)) {
            log.warn("Webhook com assinatura malformada");
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(tsRaw);
        } catch (NumberFormatException ex) {
            log.warn("Webhook com timestamp de assinatura invalido");
            return false;
        }

        long now = Instant.now().getEpochSecond();
        long delta = Math.abs(now - timestamp);
        if (delta > webhookReplayWindowSeconds) {
            log.warn("Webhook rejeitado por replay window. delta={}s", delta);
            return false;
        }

        String manifest = "id:" + resourceId + ";request-id:" + xRequestId + ";ts:" + tsRaw + ";";
        String expected = hmacSha256Hex(manifest, webhookSecret);

        return secureEquals(expected, v1.toLowerCase(Locale.ROOT));
    }

    private Map<String, String> parseSignatureHeader(String signatureHeader) {
        Map<String, String> parts = new HashMap<>();
        String[] tokens = signatureHeader.split(",");
        for (String token : tokens) {
            String[] kv = token.trim().split("=", 2);
            if (kv.length == 2) {
                parts.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim().toLowerCase(Locale.ROOT));
            }
        }
        return parts;
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao calcular assinatura HMAC do webhook.", ex);
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }

        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(StandardCharsets.UTF_8);

        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao autenticado.");
        }
        if (!"CUSTOMER".equalsIgnoreCase(user.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem consultar os proprios pagamentos.");
        }
        return getMyPayments(user.getId());
    }

    @Transactional
    public TransactionDTO createPaymentByFirebaseUid(UUID appointmentId, String firebaseUid, String paymentMethod) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao autenticado.");
        }
        if (!"CUSTOMER".equalsIgnoreCase(user.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas clientes podem criar pagamentos.");
        }
        return createPayment(appointmentId, user.getId(), paymentMethod);
    }

    @Transactional(readOnly = true)
    public MpConnectionStatusDTO getMpConnectionStatusByFirebaseUid(String firebaseUid) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        validateOwnerBarber(user);
        return userServiceClient.getBarberMpStatus(user.getId());
    }

    @Transactional
    public void disconnectMpByFirebaseUid(String firebaseUid) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        validateOwnerBarber(user);
        userServiceClient.disconnectBarberMp(user.getId());
    }

    @Transactional(readOnly = true)
    public FinancialOverviewDTO getBarbershopOverviewByFirebaseUid(
            String firebaseUid,
            UUID barbershopId,
            LocalDate from,
            LocalDate to) {
        if (!canAccessBarbershopFinancials(firebaseUid, barbershopId, false)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissao para acessar o financeiro desta barbearia.");
        }
        return getBarbershopOverview(barbershopId, from, to);
    }

    @Transactional(readOnly = true)
    public FinancialSeriesDTO getBarbershopSeriesByFirebaseUid(
            String firebaseUid,
            UUID barbershopId,
            LocalDate from,
            LocalDate to,
            String groupBy) {
        if (!canAccessBarbershopFinancials(firebaseUid, barbershopId, true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A serie financeira e restrita ao owner da barbearia.");
        }
        return getBarbershopSeries(barbershopId, from, to, groupBy);
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

        if (startDate.equals(endDate)) {
            DashboardKpiDaily kpi = dashboardKpiDailyRepository
                    .findByBarbershopIdAndReferenceDate(barbershopId, startDate)
                    .orElse(null);
            if (kpi != null && kpi.getApprovedRevenue() != null) {
                serviceRevenue = kpi.getApprovedRevenue();
            }
        }

        List<AppointmentInfoDTO> walkInAppointments = getWalkInAppointments(barbershopId, start, end);
        BigDecimal walkInRevenue = walkInAppointments.stream()
            .map(AppointmentInfoDTO::totalPrice)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalServiceRevenue = serviceRevenue.add(walkInRevenue);

        InventoryFinancialSummaryDTO inventorySummary;
        try {
            inventorySummary = productServiceClient.getFinancialSummary(
                    barbershopId,
                    startDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                    endDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (Exception ex) {
            log.warn("event=inventory-summary-fetch-failed barbershopId={} cause={}",
                    maskIdentifier(barbershopId),
                    ex.getClass().getSimpleName());
            inventorySummary = new InventoryFinancialSummaryDTO(barbershopId, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal productExpenses = inventorySummary.productExpenses() != null ? inventorySummary.productExpenses() : BigDecimal.ZERO;
        BigDecimal inventoryAssetValue = inventorySummary.inventoryAssetValue() != null ? inventorySummary.inventoryAssetValue() : BigDecimal.ZERO;
        BigDecimal operationalResult = serviceRevenue.subtract(productExpenses);
        BigDecimal operationalResultWithWalkIn = totalServiceRevenue.subtract(productExpenses);

        return new FinancialOverviewDTO(
                barbershopId,
                "BRL",
                serviceRevenue,
            walkInRevenue,
            totalServiceRevenue,
                productExpenses,
                inventoryAssetValue,
                operationalResult,
            operationalResultWithWalkIn,
                periodTransactions.size(),
            walkInAppointments.size(),
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

        String requestedGroupBy = groupBy == null ? "DAY" : groupBy.toUpperCase(Locale.ROOT);
        final String safeGroupBy = "WEEK".equals(requestedGroupBy) ? "WEEK" : "DAY";

        List<Transaction> periodTransactions = transactionRepository.findByBarbershopIdAndCreatedAtBetween(barbershopId, start, end)
                .stream()
                .filter(t -> t.getStatus() == PaymentStatus.APPROVED)
                .toList();

        Map<LocalDate, List<Transaction>> transactionsByPeriod = periodTransactions.stream()
            .collect(Collectors.groupingBy(t -> resolveGroupDate(t.getCreatedAt(), safeGroupBy)));

        List<AppointmentInfoDTO> walkInAppointments = getWalkInAppointments(barbershopId, start, end);
        Map<LocalDate, List<AppointmentInfoDTO>> walkInsByPeriod = walkInAppointments.stream()
            .collect(Collectors.groupingBy(a -> resolveGroupDate(a.startTime(), safeGroupBy)));

        Set<LocalDate> allPeriods = new HashSet<>();
        allPeriods.addAll(transactionsByPeriod.keySet());
        allPeriods.addAll(walkInsByPeriod.keySet());

        List<FinancialSeriesPointDTO> points = allPeriods.stream()
            .map(period -> {
                List<Transaction> txPoints = transactionsByPeriod.getOrDefault(period, List.of());
                BigDecimal serviceRevenue = txPoints.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                int approvedTransactions = txPoints.size();

                List<AppointmentInfoDTO> walkInPoints = walkInsByPeriod.getOrDefault(period, List.of());
                BigDecimal walkInRevenue = walkInPoints.stream()
                    .map(AppointmentInfoDTO::totalPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                int walkInAppointmentsCount = walkInPoints.size();

                return new FinancialSeriesPointDTO(
                    period,
                    serviceRevenue,
                    walkInRevenue,
                    serviceRevenue.add(walkInRevenue),
                    approvedTransactions,
                    walkInAppointmentsCount
                );
            })
            .sorted(Comparator.comparing(FinancialSeriesPointDTO::date))
            .toList();

        return new FinancialSeriesDTO(barbershopId, safeGroupBy, points);
        }

        private List<AppointmentInfoDTO> getWalkInAppointments(UUID barbershopId, LocalDateTime from, LocalDateTime to) {
        try {
            return scheduleServiceClient.getBarbershopAppointmentsByPeriod(
                        barbershopId,
                        from.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        to.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .stream()
                .filter(this::isWalkInForFinancialReport)
                .toList();
        } catch (Exception ex) {
            log.warn("event=walkin-appointments-fetch-failed barbershopId={} cause={}",
                    maskIdentifier(barbershopId),
                    ex.getClass().getSimpleName());
            return List.of();
        }
        }

        private boolean isWalkInForFinancialReport(AppointmentInfoDTO appointment) {
        if (appointment == null || appointment.customerId() == null || appointment.startTime() == null) {
            return false;
        }

        String status = appointment.status() == null ? "" : appointment.status().toUpperCase(Locale.ROOT);
        return WALK_IN_CUSTOMER_ID.equals(appointment.customerId())
            && !"CANCELLED".equals(status)
            && !"NO_SHOW".equals(status);
        }

        private LocalDate resolveGroupDate(LocalDateTime dateTime, String safeGroupBy) {
        LocalDate date = dateTime.toLocalDate();
        if ("WEEK".equals(safeGroupBy)) {
            return date.with(java.time.DayOfWeek.MONDAY);
        }
        return date;
    }

        private void updateDailyKpiProjectionForApproved(Transaction transaction) {
            if (transaction.getBarbershopId() == null) {
                log.warn("event=kpi-daily-projection-skipped txId={} reason=missing-barbershop-id",
                        maskIdentifier(transaction.getId()));
                return;
            }

            LocalDate referenceDate = (transaction.getCreatedAt() != null)
                    ? transaction.getCreatedAt().toLocalDate()
                    : LocalDate.now();

            DashboardKpiDaily kpi = dashboardKpiDailyRepository
                    .findByBarbershopIdAndReferenceDate(transaction.getBarbershopId(), referenceDate)
                    .orElseGet(() -> DashboardKpiDaily.builder()
                            .barbershopId(transaction.getBarbershopId())
                            .referenceDate(referenceDate)
                            .approvedRevenue(BigDecimal.ZERO)
                            .approvedTransactionsCount(0)
                            .build());

            BigDecimal currentRevenue = kpi.getApprovedRevenue() == null ? BigDecimal.ZERO : kpi.getApprovedRevenue();
            BigDecimal txAmount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
            Integer currentCount = kpi.getApprovedTransactionsCount() == null ? 0 : kpi.getApprovedTransactionsCount();

            kpi.setApprovedRevenue(currentRevenue.add(txAmount));
            kpi.setApprovedTransactionsCount(currentCount + 1);

            dashboardKpiDailyRepository.save(kpi);
        }

    @Transactional(readOnly = true)
    public boolean canAccessBarbershopFinancials(UUID userId, UUID barbershopId, boolean ownerOnly) {
        UserInfoDTO user = userServiceClient.getUserById(userId);
        return checkBarbershopAccess(user, barbershopId, ownerOnly);
    }

    private String maskIdentifier(Object value) {
        if (value == null) {
            return "***";
        }

        String normalized = value.toString().trim();
        if (normalized.length() <= 6) {
            return "***";
        }

        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 2);
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

    private void validateOwnerBarber(UserInfoDTO user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario nao autenticado.");
        }
        if (!"BARBER".equalsIgnoreCase(user.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas barbeiros podem gerenciar o Mercado Pago.");
        }

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase(Locale.ROOT);
        if (!role.contains("OWNER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas owner pode vincular/desvincular conta Mercado Pago.");
        }
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
