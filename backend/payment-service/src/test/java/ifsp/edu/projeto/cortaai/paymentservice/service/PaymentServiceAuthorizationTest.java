package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.paymentservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cobre os ramos de autorização de checkBarbershopAccess e métodos que
 * delegam a canAccessBarbershopFinancials.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceAuthorizationTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WebhookLogRepository webhookLogRepository;
    @Mock private DashboardKpiDailyRepository dashboardKpiDailyRepository;
    @Mock private ScheduleServiceClient scheduleServiceClient;
    @Mock private BarbershopServiceClient barbershopServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private ProductServiceClient productServiceClient;
    @Mock private MercadoPagoAuthorizationClient mercadoPagoAuthorizationClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    // ─── checkBarbershopAccess: ramos de guarda ───────────────────────────────

    @Test
    void canAccessReturnsFalseWhenUserIsNull() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(null);

        assertThat(paymentService.canAccessBarbershopFinancials("uid", shopId, false)).isFalse();
    }

    @Test
    void canAccessReturnsFalseWhenUserTypeIsNull() {
        UUID shopId = UUID.randomUUID();
        UserInfoDTO user = new UserInfoDTO();
        user.setId(UUID.randomUUID());
        user.setUserType(null);
        user.setBarbershopId(shopId);
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(user);

        assertThat(paymentService.canAccessBarbershopFinancials("uid", shopId, false)).isFalse();
    }

    @Test
    void canAccessReturnsFalseForCustomer() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(barber(UUID.randomUUID(), "CUSTOMER", null, shopId));

        assertThat(paymentService.canAccessBarbershopFinancials("uid", shopId, false)).isFalse();
    }

    @Test
    void canAccessReturnsFalseWhenOwnerOnlyButStaff() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(barber(UUID.randomUUID(), "BARBER", "STAFF", shopId));

        assertThat(paymentService.canAccessBarbershopFinancials("uid", shopId, true)).isFalse();
    }

    @Test
    void canAccessReturnsFalseWhenBarbershopIdMismatch() {
        UUID shopId = UUID.randomUUID();
        UUID otherShop = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(barber(UUID.randomUUID(), "BARBER", "OWNER", otherShop));

        assertThat(paymentService.canAccessBarbershopFinancials("uid", shopId, true)).isFalse();
    }

    @Test
    void canAccessReturnsTrueForStaffBarberOnNonOwnerOnlyCheck() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(barber(UUID.randomUUID(), "BARBER", "STAFF", shopId));

        assertThat(paymentService.canAccessBarbershopFinancials("uid", shopId, false)).isTrue();
    }

    @Test
    void canAccessByUserIdReturnsTrueForOwner() {
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(userServiceClient.getUserById(ownerId)).thenReturn(barber(ownerId, "BARBER", "OWNER", shopId));

        assertThat(paymentService.canAccessBarbershopFinancials(ownerId, shopId, true)).isTrue();
    }

    // ─── getBarbershopOverviewByFirebaseUid: acesso negado ───────────────────

    @Test
    void overviewByFirebaseThrows403WhenCustomerTriesToAccess() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("cust")).thenReturn(barber(UUID.randomUUID(), "CUSTOMER", null, shopId));

        assertThatThrownBy(() -> paymentService.getBarbershopOverviewByFirebaseUid(
                "cust", shopId, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void overviewByFirebaseThrows403WhenBarberAccessesWrongShop() {
        UUID shopId = UUID.randomUUID();
        UUID otherShop = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("barber")).thenReturn(barber(UUID.randomUUID(), "BARBER", "OWNER", otherShop));

        assertThatThrownBy(() -> paymentService.getBarbershopOverviewByFirebaseUid(
                "barber", shopId, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ─── getBarbershopSeriesByFirebaseUid: acesso negado ─────────────────────

    @Test
    void seriesByFirebaseThrows403WhenStaffBarberTriesToAccess() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("staff")).thenReturn(barber(UUID.randomUUID(), "BARBER", "STAFF", shopId));

        assertThatThrownBy(() -> paymentService.getBarbershopSeriesByFirebaseUid(
                "staff", shopId, LocalDate.now(), LocalDate.now(), "DAY"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void seriesByFirebaseThrows403WhenOwnerAccessesWrongShop() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("owner")).thenReturn(barber(UUID.randomUUID(), "BARBER", "OWNER", UUID.randomUUID()));

        assertThatThrownBy(() -> paymentService.getBarbershopSeriesByFirebaseUid(
                "owner", shopId, LocalDate.now(), LocalDate.now(), "DAY"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ─── getBarberFinancialPerformance: acesso negado + overload sem datas ───

    @Test
    void performanceThrows403WhenStaffTriesToAccess() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("staff")).thenReturn(barber(UUID.randomUUID(), "BARBER", "STAFF", shopId));

        assertThatThrownBy(() -> paymentService.getBarberFinancialPerformance(
                "staff", shopId, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void performanceNoDateOverloadDelegatesToDateOverload() {
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UserInfoDTO owner = barber(ownerId, "BARBER", "OWNER", shopId);

        when(userServiceClient.getUserByFirebaseUid("owner")).thenReturn(owner);
        when(scheduleServiceClient.getBarbershopAppointmentsByPeriod(eq(shopId), any(), any()))
                .thenReturn(List.of());

        List<BarberFinancialPerformanceResponseDTO> result =
                paymentService.getBarberFinancialPerformance("owner", shopId);

        assertThat(result).isEmpty();
    }

    // ─── getBarberFinancialSummaryByFirebaseUid: acesso negado ───────────────

    @Test
    void summaryThrows403WhenCustomerTriesToAccess() {
        UUID shopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("cust")).thenReturn(barber(UUID.randomUUID(), "CUSTOMER", null, shopId));

        assertThatThrownBy(() -> paymentService.getBarberFinancialSummaryByFirebaseUid(
                "cust", shopId, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private UserInfoDTO barber(UUID id, String type, String role, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setUserType(type);
        user.setRole(role);
        user.setBarbershopId(shopId);
        return user;
    }
}
