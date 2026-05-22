package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentActivityInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.model.DashboardKpiDaily;
import ifsp.edu.projeto.cortaai.paymentservice.model.PaymentStatus;
import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceInternalHelpersTest {

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
    void shouldMapMercadoPagoStatuses() {
        assertThat((PaymentStatus) ReflectionTestUtils.invokeMethod(paymentService, "mapMpStatus", "approved"))
                .isEqualTo(PaymentStatus.APPROVED);
        assertThat((PaymentStatus) ReflectionTestUtils.invokeMethod(paymentService, "mapMpStatus", "rejected"))
                .isEqualTo(PaymentStatus.REJECTED);
        assertThat((PaymentStatus) ReflectionTestUtils.invokeMethod(paymentService, "mapMpStatus", "cancelled"))
                .isEqualTo(PaymentStatus.CANCELLED);
        assertThat((PaymentStatus) ReflectionTestUtils.invokeMethod(paymentService, "mapMpStatus", "refunded"))
                .isEqualTo(PaymentStatus.REFUNDED);
        assertThat((PaymentStatus) ReflectionTestUtils.invokeMethod(paymentService, "mapMpStatus", "authorized"))
                .isEqualTo(PaymentStatus.IN_PROCESS);
        assertThat((PaymentStatus) ReflectionTestUtils.invokeMethod(paymentService, "mapMpStatus", "unknown"))
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldBuildPaymentReturnUrlsWithFallback() {
        ReflectionTestUtils.setField(paymentService, "postConnectRedirectUrl", "https://app.example.com/barberHome?mpLinked=true");

        assertThat((String) ReflectionTestUtils.invokeMethod(paymentService, "paymentReturnUrl", "success"))
                .isEqualTo("https://app.example.com/meus-agendamentos?payment=success");

        ReflectionTestUtils.setField(paymentService, "postConnectRedirectUrl", "::bad-uri::");

        assertThat((String) ReflectionTestUtils.invokeMethod(paymentService, "paymentReturnUrl", "failure"))
                .isEqualTo("https://web.cortaai.shop/meus-agendamentos?payment=failure");
    }

    @Test
    void shouldCalculateGrossCommissionAndNonNegativeValues() {
        UUID serviceId = UUID.randomUUID();
        AppointmentInfoDTO appointmentWithTotal = appointment(UUID.randomUUID(), new BigDecimal("49.999"), "CONCLUDED",
                List.of(activity(serviceId, new BigDecimal("40.00"))));
        AppointmentInfoDTO appointmentWithActivities = appointment(UUID.randomUUID(), null, "CONCLUDED",
                List.of(activity(serviceId, new BigDecimal("40.00")), activity(UUID.randomUUID(), null)));
        AppointmentInfoDTO appointmentWithoutActivities = appointment(UUID.randomUUID(), null, "CONCLUDED", List.of());

        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "calculateGrossAmount",
                appointmentWithTotal, new BigDecimal("30.129"))).isEqualByComparingTo("30.13");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "calculateGrossAmount",
                appointmentWithTotal, null)).isEqualByComparingTo("50.00");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "calculateGrossAmount",
                appointmentWithActivities, null)).isEqualByComparingTo("40.00");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "calculateGrossAmount",
                appointmentWithoutActivities, null)).isEqualByComparingTo(BigDecimal.ZERO);

        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "calculateCommission",
                appointmentWithActivities, Map.of(serviceId, new BigDecimal("25.00")))).isEqualByComparingTo("10.00");
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "calculateCommission",
                appointmentWithoutActivities, Map.of())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat((BigDecimal) ReflectionTestUtils.invokeMethod(paymentService, "nonNegative",
                new BigDecimal("-1.00"))).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldFilterAppointmentsForReports() {
        AppointmentInfoDTO validPerformance = appointment(UUID.randomUUID(), BigDecimal.TEN, "IN_PROGRESS", List.of());
        AppointmentInfoDTO invalidPerformance = new AppointmentInfoDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Cliente",
                "Barbeiro",
                "Barbearia",
                LocalDateTime.of(2026, 5, 22, 10, 0),
                BigDecimal.TEN,
                "CONCLUDED",
                List.of()
        );
        AppointmentInfoDTO validWalkIn = appointment(WALK_IN_CUSTOMER_ID, BigDecimal.TEN, "WALK_IN", List.of());
        AppointmentInfoDTO noShowWalkIn = appointment(WALK_IN_CUSTOMER_ID, BigDecimal.TEN, "NO_SHOW", List.of());

        assertThat((Boolean) ReflectionTestUtils.invokeMethod(paymentService, "isAppointmentForBarberPerformance", validPerformance))
                .isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(paymentService, "isAppointmentForBarberPerformance", invalidPerformance))
                .isFalse();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(paymentService, "isAppointmentForBarberPerformance", (Object) null))
                .isFalse();

        assertThat((Boolean) ReflectionTestUtils.invokeMethod(paymentService, "isWalkInForFinancialReport", validWalkIn))
                .isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(paymentService, "isWalkInForFinancialReport", noShowWalkIn))
                .isFalse();
        assertThat((LocalDate) ReflectionTestUtils.invokeMethod(paymentService, "resolveGroupDate",
                LocalDateTime.of(2026, 5, 6, 10, 0), "DAY")).isEqualTo(LocalDate.of(2026, 5, 6));
    }

    @Test
    void shouldUpdateDailyKpiProjectionForApprovedTransactions() {
        UUID shopId = UUID.randomUUID();
        Transaction withoutShop = Transaction.builder().id(UUID.randomUUID()).amount(BigDecimal.TEN).build();

        ReflectionTestUtils.invokeMethod(paymentService, "updateDailyKpiProjectionForApproved", withoutShop);

        verify(dashboardKpiDailyRepository, never()).save(any());

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .amount(null)
                .createdAt(null)
                .build();
        DashboardKpiDaily existing = DashboardKpiDaily.builder()
                .barbershopId(shopId)
                .referenceDate(LocalDate.now())
                .approvedRevenue(null)
                .approvedTransactionsCount(null)
                .build();

        when(dashboardKpiDailyRepository.findByBarbershopIdAndReferenceDate(eq(shopId), any()))
                .thenReturn(Optional.of(existing));

        ReflectionTestUtils.invokeMethod(paymentService, "updateDailyKpiProjectionForApproved", transaction);

        ArgumentCaptor<DashboardKpiDaily> captor = ArgumentCaptor.forClass(DashboardKpiDaily.class);
        verify(dashboardKpiDailyRepository).save(captor.capture());
        assertThat(captor.getValue().getApprovedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getApprovedTransactionsCount()).isEqualTo(1);
    }

    @Test
    void shouldValidateOwnerBarberFailuresThroughPublicStatusEndpoint() {
        when(userServiceClient.getUserByFirebaseUid("missing")).thenReturn(null);
        when(userServiceClient.getUserByFirebaseUid("customer")).thenReturn(user(UUID.randomUUID(), "CUSTOMER", null, UUID.randomUUID()));

        assertThatThrownBy(() -> paymentService.getMpConnectionStatusByFirebaseUid("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
        assertThatThrownBy(() -> paymentService.getMpConnectionStatusByFirebaseUid("customer"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void shouldRunOwnerOverviewPathWithInventorySuccess() {
        UUID shopId = UUID.randomUUID();
        UserInfoDTO owner = user(UUID.randomUUID(), "BARBER", "OWNER", shopId);
        LocalDate day = LocalDate.of(2026, 5, 22);

        when(userServiceClient.getUserByFirebaseUid("owner")).thenReturn(owner);
        when(transactionRepository.findByBarbershopIdAndCreatedAtBetween(eq(shopId), any(), any())).thenReturn(List.of());
        when(transactionRepository.sumAmountByBarbershopAndStatusAndCreatedAtBetween(eq(shopId), eq(PaymentStatus.APPROVED), any(), any()))
                .thenReturn(new BigDecimal("200.00"));
        when(dashboardKpiDailyRepository.findByBarbershopIdAndReferenceDate(shopId, day)).thenReturn(Optional.empty());
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any())).thenReturn(List.of());
        when(productServiceClient.getFinancialSummary(shopId, "2026-05-22", "2026-05-22"))
                .thenReturn(new InventoryFinancialSummaryDTO(shopId, new BigDecimal("30.00"), new BigDecimal("500.00")));

        FinancialOverviewDTO overview = paymentService.getBarbershopOverviewByFirebaseUid("owner", shopId, day, day);

        assertThat(overview.serviceRevenue()).isEqualByComparingTo("200.00");
        assertThat(overview.productExpenses()).isEqualByComparingTo("30.00");
        assertThat(overview.inventoryAssetValue()).isEqualByComparingTo("500.00");
        assertThat(overview.operationalResult()).isEqualByComparingTo("170.00");
    }

    private UserInfoDTO user(UUID id, String type, String role, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setUserType(type);
        user.setRole(role);
        user.setBarbershopId(shopId);
        return user;
    }

    private AppointmentInfoDTO appointment(UUID customerId,
                                           BigDecimal total,
                                           String status,
                                           List<AppointmentActivityInfoDTO> activities) {
        return new AppointmentInfoDTO(
                UUID.randomUUID(),
                customerId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cliente",
                "Barbeiro",
                "Barbearia",
                LocalDateTime.of(2026, 5, 22, 10, 0),
                total,
                status,
                activities
        );
    }

    private AppointmentActivityInfoDTO activity(UUID activityId, BigDecimal price) {
        return new AppointmentActivityInfoDTO(UUID.randomUUID(), activityId, "Servico", price, 30);
    }
}
