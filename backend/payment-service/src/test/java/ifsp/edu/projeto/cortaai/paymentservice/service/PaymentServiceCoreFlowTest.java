package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.model.PaymentStatus;
import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import ifsp.edu.projeto.cortaai.paymentservice.model.WebhookLog;
import ifsp.edu.projeto.cortaai.paymentservice.repository.DashboardKpiDailyRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.TransactionRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.WebhookLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceCoreFlowTest {

    private static final UUID WALK_IN_CUSTOMER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WebhookLogRepository webhookLogRepository;
    @Mock
    private DashboardKpiDailyRepository dashboardKpiDailyRepository;
    @Mock
    private ScheduleServiceClient scheduleServiceClient;
    @Mock
    private BarbershopServiceClient barbershopServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private MercadoPagoAuthorizationClient mercadoPagoAuthorizationClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldReturnTransactionByIdAndRejectMissingTransaction() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = transaction(UUID.randomUUID(), UUID.randomUUID(), PaymentStatus.PENDING,
                new BigDecimal("45.00"), LocalDateTime.of(2026, 5, 22, 8, 0));
        transaction.setId(transactionId);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        TransactionDTO dto = paymentService.getById(transactionId);

        assertThat(dto.id()).isEqualTo(transactionId);
        assertThat(dto.amount()).isEqualByComparingTo("45.00");

        UUID missingId = UUID.randomUUID();
        when(transactionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById(missingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Transação não encontrada: " + missingId);
    }

    @Test
    void shouldRejectUnauthenticatedFirebaseFlows() {
        when(userServiceClient.getUserByFirebaseUid("missing")).thenReturn(null);

        assertThatThrownBy(() -> paymentService.getMyPaymentsByFirebaseUid("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
        assertThatThrownBy(() -> paymentService.createPaymentByFirebaseUid(UUID.randomUUID(), "missing", "PIX"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void shouldRejectCreatePaymentForNonCustomerAndDuplicatePayment() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("barber"))
                .thenReturn(user(UUID.randomUUID(), "BARBER", "OWNER", UUID.randomUUID()));

        assertThatThrownBy(() -> paymentService.createPaymentByFirebaseUid(appointmentId, "barber", "PIX"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        Transaction existing = transaction(customerId, UUID.randomUUID(), PaymentStatus.PENDING,
                new BigDecimal("30.00"), LocalDateTime.now());
        when(transactionRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> paymentService.createPayment(appointmentId, customerId, "PIX"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Já existe um pagamento para este agendamento");
    }

    @Test
    void shouldRejectPaymentWhenAppointmentDoesNotBelongToCustomer() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        AppointmentInfoDTO appointment = appointment(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 22, 9, 0),
                new BigDecimal("60.00"),
                "CONFIRMED");

        when(transactionRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(scheduleServiceClient.getAppointmentById(appointmentId)).thenReturn(appointment);

        assertThatThrownBy(() -> paymentService.createPayment(appointmentId, customerId, "PIX"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Este agendamento não pertence ao usuário");
    }

    @Test
    void shouldSkipAlreadyProcessedWebhookAndMarkNonPaymentEventsAsProcessed() {
        when(webhookLogRepository.existsByMpResourceIdAndProcessedTrue("resource-1")).thenReturn(true);

        paymentService.processWebhook("resource-1", "payment", "{}");

        verify(webhookLogRepository, never()).save(any(WebhookLog.class));

        when(webhookLogRepository.existsByMpResourceIdAndProcessedTrue("resource-2")).thenReturn(false);

        paymentService.processWebhook("resource-2", "merchant_order", "{\"id\":\"resource-2\"}");

        ArgumentCaptor<WebhookLog> captor = ArgumentCaptor.forClass(WebhookLog.class);
        verify(webhookLogRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2);
        assertThat(captor.getAllValues().get(1).isProcessed()).isTrue();
        assertThat(captor.getAllValues().get(1).getEventType()).isEqualTo("merchant_order");
    }

    @Test
    void shouldReturnEmptySeriesWhenNoFinancialEventsExistAndNormalizeGroupBy() {
        UUID shopId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 2);

        when(transactionRepository.findByBarbershopIdAndCreatedAtBetween(eq(shopId), any(), any()))
                .thenReturn(List.of(transaction(UUID.randomUUID(), shopId, PaymentStatus.CANCELLED,
                        new BigDecimal("30.00"), from.atTime(10, 0))));
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any()))
                .thenReturn(Arrays.asList(
                        appointment(UUID.randomUUID(), shopId, WALK_IN_CUSTOMER_ID, UUID.randomUUID(),
                                from.atTime(12, 0), new BigDecimal("50.00"), "NO_SHOW"),
                        null
                ));

        FinancialSeriesDTO series = paymentService.getBarbershopSeries(shopId, from, to, "month");

        assertThat(series.groupBy()).isEqualTo("DAY");
        assertThat(series.points()).isEmpty();
    }

    @Test
    void shouldDenyOwnerOnlyFinancialAccessForStaffAndMismatchedShop() {
        UUID shopId = UUID.randomUUID();
        UUID otherShopId = UUID.randomUUID();
        UserInfoDTO staff = user(UUID.randomUUID(), "BARBER", "STAFF", shopId);
        UserInfoDTO ownerFromOtherShop = user(UUID.randomUUID(), "BARBER", "OWNER", otherShopId);

        when(userServiceClient.getUserByFirebaseUid("staff")).thenReturn(staff);
        when(userServiceClient.getUserById(ownerFromOtherShop.getId())).thenReturn(ownerFromOtherShop);

        assertThat(paymentService.canAccessBarbershopFinancials("staff", shopId, true)).isFalse();
        assertThat(paymentService.canAccessBarbershopFinancials(ownerFromOtherShop.getId(), shopId, true)).isFalse();

        assertThatThrownBy(() -> paymentService.getBarbershopSeriesByFirebaseUid("staff", shopId, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(transactionRepository, never()).findByBarbershopIdAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    void shouldReturnEmptyBarberPerformanceWhenScheduleServiceFails() {
        UUID shopId = UUID.randomUUID();
        UserInfoDTO owner = user(UUID.randomUUID(), "BARBER", "OWNER", shopId);

        when(userServiceClient.getUserByFirebaseUid("owner")).thenReturn(owner);
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any()))
                .thenThrow(new RuntimeException("schedule offline"));

        assertThat(paymentService.getBarberFinancialPerformance("owner", shopId, null, null)).isEmpty();
    }

    private UserInfoDTO user(UUID id, String type, String role, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setUserType(type);
        user.setRole(role);
        user.setBarbershopId(shopId);
        return user;
    }

    private Transaction transaction(UUID customerId, UUID shopId, PaymentStatus status, BigDecimal amount, LocalDateTime createdAt) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .appointmentId(UUID.randomUUID())
                .customerId(customerId)
                .barbershopId(shopId)
                .amount(amount)
                .grossAmount(amount)
                .platformFeeAmount(BigDecimal.ZERO)
                .paymentMethod("PIX")
                .status(status)
                .checkoutUrl("https://checkout.test")
                .createdAt(createdAt)
                .build();
    }

    private AppointmentInfoDTO appointment(UUID id,
                                           UUID shopId,
                                           UUID customerId,
                                           UUID barberId,
                                           LocalDateTime startTime,
                                           BigDecimal total,
                                           String status) {
        return new AppointmentInfoDTO(
                id,
                customerId,
                barberId,
                shopId,
                "Cliente",
                "Barbeiro",
                "Barbearia",
                startTime,
                total,
                status,
                List.of()
        );
    }
}
