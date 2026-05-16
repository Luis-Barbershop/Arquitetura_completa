package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentActivityInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.CommissionRuleInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.MpConnectionStatusDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.SaveMpCredentialsDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceFinancialsTest {

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
    void shouldReturnPaymentsForAuthenticatedCustomer() {
        UUID customerId = UUID.randomUUID();
        UserInfoDTO user = user(customerId, "CUSTOMER", null, null);
        Transaction tx = transaction(customerId, UUID.randomUUID(), PaymentStatus.APPROVED, new BigDecimal("40.00"),
                LocalDateTime.of(2026, 5, 4, 10, 0));

        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user);
        when(transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(tx));

        List<TransactionDTO> result = paymentService.getMyPaymentsByFirebaseUid("firebase-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).customerId()).isEqualTo(customerId);
        assertThat(result.get(0).status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void shouldRejectPaymentLookupWhenFirebaseUserIsNotCustomer() {
        when(userServiceClient.getUserByFirebaseUid("firebase-1"))
                .thenReturn(user(UUID.randomUUID(), "BARBER", "OWNER", UUID.randomUUID()));

        assertThatThrownBy(() -> paymentService.getMyPaymentsByFirebaseUid("firebase-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
    }

    @Test
    void shouldValidateOwnerBeforeReadingAndDisconnectingMercadoPagoStatus() {
        UUID ownerId = UUID.randomUUID();
        UserInfoDTO owner = user(ownerId, "BARBER", "SHOP_OWNER", UUID.randomUUID());
        MpConnectionStatusDTO status = new MpConnectionStatusDTO(true, "123***99", true);

        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner);
        when(userServiceClient.getBarberMpStatus(ownerId)).thenReturn(status);
        when(userServiceClient.getBarberMpCredentials(ownerId))
                .thenReturn(new SaveMpCredentialsDTO("mp-token", "refresh-token", "1234567899", "public-key"));

        assertThat(paymentService.getMpConnectionStatusByFirebaseUid("owner-firebase")).isEqualTo(status);

        paymentService.disconnectMpByFirebaseUid("owner-firebase");

        verify(mercadoPagoAuthorizationClient).revokeSellerAuthorization(
                new SaveMpCredentialsDTO("mp-token", "refresh-token", "1234567899", "public-key"));
        verify(userServiceClient).disconnectBarberMp(ownerId);
    }

    @Test
    void shouldClearMercadoPagoLocallyWhenThereAreNoStoredCredentials() {
        UUID ownerId = UUID.randomUUID();
        UserInfoDTO owner = user(ownerId, "BARBER", "SHOP_OWNER", UUID.randomUUID());

        when(userServiceClient.getUserByFirebaseUid("owner-firebase")).thenReturn(owner);
        when(userServiceClient.getBarberMpCredentials(ownerId))
                .thenReturn(new SaveMpCredentialsDTO(null, null, null, null));

        paymentService.disconnectMpByFirebaseUid("owner-firebase");

        verify(mercadoPagoAuthorizationClient).revokeSellerAuthorization(
                new SaveMpCredentialsDTO(null, null, null, null));
        verify(userServiceClient).disconnectBarberMp(ownerId);
    }

    @Test
    void shouldRejectMercadoPagoManagementForNonOwnerBarber() {
        when(userServiceClient.getUserByFirebaseUid("barber-firebase"))
                .thenReturn(user(UUID.randomUUID(), "BARBER", "STAFF", UUID.randomUUID()));

        assertThatThrownBy(() -> paymentService.disconnectMpByFirebaseUid("barber-firebase"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");
        verify(mercadoPagoAuthorizationClient, never()).revokeSellerAuthorization(any());
    }

    @Test
    void shouldCalculateOverviewWithKpiWalkInsAndInventoryFallback() {
        UUID shopId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 5, 4);
        Transaction approved = transaction(UUID.randomUUID(), shopId, PaymentStatus.APPROVED, new BigDecimal("100.00"), day.atTime(9, 0));
        Transaction pending = transaction(UUID.randomUUID(), shopId, PaymentStatus.PENDING, new BigDecimal("50.00"), day.atTime(10, 0));
        Transaction cancelled = transaction(UUID.randomUUID(), shopId, PaymentStatus.CANCELLED, new BigDecimal("30.00"), day.atTime(11, 0));
        DashboardKpiDaily kpi = DashboardKpiDaily.builder()
                .barbershopId(shopId)
                .referenceDate(day)
                .approvedRevenue(new BigDecimal("150.00"))
                .approvedTransactionsCount(3)
                .build();
        AppointmentInfoDTO walkIn = appointment(shopId, WALK_IN_CUSTOMER_ID, day.atTime(12, 0), new BigDecimal("70.00"), "CONCLUDED");
        AppointmentInfoDTO cancelledWalkIn = appointment(shopId, WALK_IN_CUSTOMER_ID, day.atTime(13, 0), new BigDecimal("90.00"), "CANCELLED");

        when(transactionRepository.findByBarbershopIdAndCreatedAtBetween(eq(shopId), any(), any()))
                .thenReturn(List.of(approved, pending, cancelled));
        when(transactionRepository.sumAmountByBarbershopAndStatusAndCreatedAtBetween(eq(shopId), eq(PaymentStatus.APPROVED), any(), any()))
                .thenReturn(new BigDecimal("100.00"));
        when(dashboardKpiDailyRepository.findByBarbershopIdAndReferenceDate(shopId, day))
                .thenReturn(Optional.of(kpi));
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any()))
                .thenReturn(List.of(walkIn, cancelledWalkIn));
        doThrow(new RuntimeException("inventory offline"))
                .when(productServiceClient).getFinancialSummary(eq(shopId), eq("2026-05-04"), eq("2026-05-04"));

        FinancialOverviewDTO overview = paymentService.getBarbershopOverview(shopId, day, day);

        assertThat(overview.serviceRevenue()).isEqualByComparingTo("150.00");
        assertThat(overview.walkInRevenue()).isEqualByComparingTo("70.00");
        assertThat(overview.totalServiceRevenue()).isEqualByComparingTo("220.00");
        assertThat(overview.productExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(overview.approvedCount()).isEqualTo(1);
        assertThat(overview.pendingCount()).isEqualTo(1);
        assertThat(overview.cancelledCount()).isEqualTo(1);
        assertThat(overview.walkInAppointmentsCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnCommissionOverviewForStaffBarber() {
        UUID shopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID haircutId = UUID.randomUUID();
        UUID beardId = UUID.randomUUID();
        LocalDate day = LocalDate.of(2026, 5, 4);
        UserInfoDTO staff = user(barberId, "BARBER", "STAFF", shopId);

        Transaction approved = transactionWithAppointment(UUID.randomUUID(), shopId, PaymentStatus.APPROVED,
                new BigDecimal("100.00"), day.atTime(9, 0), UUID.randomUUID());
        Transaction pending = transactionWithAppointment(UUID.randomUUID(), shopId, PaymentStatus.PENDING,
                new BigDecimal("80.00"), day.atTime(10, 0), UUID.randomUUID());
        Transaction otherBarberApproved = transactionWithAppointment(UUID.randomUUID(), shopId, PaymentStatus.APPROVED,
                new BigDecimal("90.00"), day.atTime(11, 0), UUID.randomUUID());

        AppointmentInfoDTO approvedAppointment = appointment(
                approved.getAppointmentId(),
                shopId,
                UUID.randomUUID(),
                barberId,
                day.atTime(12, 0),
                new BigDecimal("100.00"),
                "CONCLUDED",
                List.of(
                        activity(haircutId, new BigDecimal("80.00")),
                        activity(beardId, new BigDecimal("20.00"))
                ));
        AppointmentInfoDTO pendingAppointment = appointment(
                pending.getAppointmentId(),
                shopId,
                UUID.randomUUID(),
                barberId,
                day.atTime(13, 0),
                new BigDecimal("80.00"),
                "CONFIRMED",
                List.of(activity(haircutId, new BigDecimal("80.00"))));
        AppointmentInfoDTO otherBarberAppointment = appointment(
                otherBarberApproved.getAppointmentId(),
                shopId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                day.atTime(14, 0),
                new BigDecimal("90.00"),
                "CONCLUDED",
                List.of(activity(haircutId, new BigDecimal("90.00"))));
        AppointmentInfoDTO walkIn = appointment(
                UUID.randomUUID(),
                shopId,
                WALK_IN_CUSTOMER_ID,
                barberId,
                day.atTime(15, 0),
                new BigDecimal("50.00"),
                "CONCLUDED",
                List.of(activity(haircutId, new BigDecimal("50.00"))));

        when(userServiceClient.getUserByFirebaseUid("staff-firebase")).thenReturn(staff);
        when(transactionRepository.findByBarbershopIdAndCreatedAtBetween(eq(shopId), any(), any()))
                .thenReturn(List.of(approved, pending, otherBarberApproved));
        when(scheduleServiceClient.getAppointmentById(approved.getAppointmentId())).thenReturn(approvedAppointment);
        when(scheduleServiceClient.getAppointmentById(pending.getAppointmentId())).thenReturn(pendingAppointment);
        when(scheduleServiceClient.getAppointmentById(otherBarberApproved.getAppointmentId())).thenReturn(otherBarberAppointment);
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any())).thenReturn(List.of(walkIn));
        when(barbershopServiceClient.getBarberCommissions(shopId, barberId)).thenReturn(List.of(
                new CommissionRuleInfoDTO(UUID.randomUUID(), haircutId, "Corte", new BigDecimal("50.00")),
                new CommissionRuleInfoDTO(UUID.randomUUID(), beardId, "Barba", new BigDecimal("30.00"))
        ));

        FinancialOverviewDTO overview = paymentService.getBarbershopOverviewByFirebaseUid(
                "staff-firebase", shopId, day, day);

        assertThat(overview.serviceRevenue()).isEqualByComparingTo("46.00");
        assertThat(overview.walkInRevenue()).isEqualByComparingTo("25.00");
        assertThat(overview.totalServiceRevenue()).isEqualByComparingTo("71.00");
        assertThat(overview.productExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(overview.approvedCount()).isEqualTo(1);
        assertThat(overview.pendingCount()).isEqualTo(1);
        assertThat(overview.walkInAppointmentsCount()).isEqualTo(1);

        BarberFinancialSummaryDTO summary = paymentService.getBarberFinancialSummaryByFirebaseUid(
                "staff-firebase", shopId, day, day);

        assertThat(summary.grossTotalRevenue()).isEqualByComparingTo("150.00");
        assertThat(summary.barberTotalCommission()).isEqualByComparingTo("71.00");
        assertThat(summary.barbershopTotalCommission()).isEqualByComparingTo("79.00");
    }

    @Test
    void shouldCalculateWeeklySeriesWithApprovedTransactionsAndWalkIns() {
        UUID shopId = UUID.randomUUID();
        LocalDate monday = LocalDate.of(2026, 5, 4);
        Transaction approved = transaction(UUID.randomUUID(), shopId, PaymentStatus.APPROVED, new BigDecimal("100.00"), monday.plusDays(2).atTime(9, 0));
        Transaction rejected = transaction(UUID.randomUUID(), shopId, PaymentStatus.REJECTED, new BigDecimal("80.00"), monday.plusDays(2).atTime(10, 0));
        AppointmentInfoDTO walkIn = appointment(shopId, WALK_IN_CUSTOMER_ID, monday.plusDays(3).atTime(11, 0), new BigDecimal("35.00"), "CONCLUDED");

        when(transactionRepository.findByBarbershopIdAndCreatedAtBetween(eq(shopId), any(), any()))
                .thenReturn(List.of(approved, rejected));
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any()))
                .thenReturn(List.of(walkIn));

        FinancialSeriesDTO series = paymentService.getBarbershopSeries(shopId, monday, monday.plusDays(6), "WEEK");

        assertThat(series.groupBy()).isEqualTo("WEEK");
        assertThat(series.points()).hasSize(1);
        assertThat(series.points().get(0).date()).isEqualTo(monday);
        assertThat(series.points().get(0).serviceRevenue()).isEqualByComparingTo("100.00");
        assertThat(series.points().get(0).walkInRevenue()).isEqualByComparingTo("35.00");
        assertThat(series.points().get(0).approvedTransactions()).isEqualTo(1);
        assertThat(series.points().get(0).walkInAppointmentsCount()).isEqualTo(1);
    }

    @Test
    void shouldAuthorizeFinancialAccessByUserIdAndFirebaseUid() {
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UserInfoDTO owner = user(ownerId, "BARBER", "OWNER", shopId);
        UserInfoDTO customer = user(UUID.randomUUID(), "CUSTOMER", null, shopId);

        when(userServiceClient.getUserById(ownerId)).thenReturn(owner);
        when(userServiceClient.getUserByFirebaseUid("customer")).thenReturn(customer);

        assertThat(paymentService.canAccessBarbershopFinancials(ownerId, shopId, true)).isTrue();
        assertThat(paymentService.canAccessBarbershopFinancials("customer", shopId, false)).isFalse();
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
        return transactionWithAppointment(customerId, shopId, status, amount, createdAt, UUID.randomUUID());
    }

    private Transaction transactionWithAppointment(UUID customerId, UUID shopId, PaymentStatus status, BigDecimal amount,
                                                   LocalDateTime createdAt, UUID appointmentId) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .appointmentId(appointmentId)
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

    private AppointmentInfoDTO appointment(UUID shopId, UUID customerId, LocalDateTime startTime, BigDecimal total, String status) {
        return appointment(UUID.randomUUID(), shopId, customerId, UUID.randomUUID(), startTime, total, status, List.of());
    }

    private AppointmentInfoDTO appointment(UUID id, UUID shopId, UUID customerId, UUID barberId, LocalDateTime startTime,
                                           BigDecimal total, String status, List<AppointmentActivityInfoDTO> activities) {
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
                activities
        );
    }

    private AppointmentActivityInfoDTO activity(UUID activityId, BigDecimal price) {
        return new AppointmentActivityInfoDTO(UUID.randomUUID(), activityId, "Servico", price, 30);
    }
}
