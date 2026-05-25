package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AgendaThermometerResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberSkillMatrixResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.model.analytics.VBarberSkillMatrix;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VBarberSkillMatrixRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.projection.AgendaThermometerProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private VBarberSkillMatrixRepository vBarberSkillMatrixRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AnalyticsService analyticsService;

    // ─── getAgendaThermometer(barbershopId) ──────────────────────────────────

    @Test
    void shouldExposeCompletedAppointmentsInAgendaThermometer() {
        when(appointmentRepository.findAgendaThermometerByBarbershopId("shop-1"))
                .thenReturn(List.of(agendaThermometerRow()));

        List<AgendaThermometerResponseDTO> result = analyticsService.getAgendaThermometer("shop-1");

        assertThat(result).hasSize(1);
        AgendaThermometerResponseDTO row = result.get(0);
        assertThat(row.totalAppointments()).isEqualTo(5);
        assertThat(row.activeAppointments()).isEqualTo(2);
        assertThat(row.walkinAppointments()).isEqualTo(1);
        assertThat(row.pendingAppointments()).isZero();
        assertThat(row.completedAppointments()).isEqualTo(2);
        assertThat(row.lostAppointments()).isZero();
    }

    // ─── getBarberSkillMatrix(barbershopId) ───────────────────────────────────

    @Test
    void shouldReturnBarberSkillMatrixForBarbershop() {
        VBarberSkillMatrix row = skillMatrixRow("barber-1", "Ana", "Corte", 10L, new BigDecimal("500.00"));
        when(vBarberSkillMatrixRepository.findByBarbershopId("shop-1")).thenReturn(List.of(row));

        List<BarberSkillMatrixResponseDTO> result = analyticsService.getBarberSkillMatrix("shop-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).barberId()).isEqualTo("barber-1");
        assertThat(result.get(0).barberName()).isEqualTo("Ana");
        assertThat(result.get(0).activityName()).isEqualTo("Corte");
        assertThat(result.get(0).timesExecuted()).isEqualTo(10L);
        assertThat(result.get(0).totalGeneratedByActivity()).isEqualByComparingTo("500.00");
    }

    static final UUID SHOP_UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    static final String SHOP_ID = SHOP_UUID.toString();

    // ─── getAgendaThermometer(firebaseUid, barbershopId) ─────────────────────

    @Test
    void shouldReturnThermometerForAuthenticatedOwner() {
        UserInfoDTO owner = ownerUser(SHOP_UUID);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(appointmentRepository.findAgendaThermometerByBarbershopId(SHOP_ID))
                .thenReturn(List.of(agendaThermometerRow()));

        List<AgendaThermometerResponseDTO> result = analyticsService.getAgendaThermometer("owner-uid", SHOP_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldThrow401WhenUserNotFoundForThermometer() {
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(null);

        assertThatThrownBy(() -> analyticsService.getAgendaThermometer("uid", SHOP_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void shouldThrow403WhenCustomerTriesToAccessThermometer() {
        UserInfoDTO customer = new UserInfoDTO();
        customer.setId(UUID.randomUUID());
        customer.setUserType("CUSTOMER");
        customer.setRole("ROLE_CUSTOMER");
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(customer);

        assertThatThrownBy(() -> analyticsService.getAgendaThermometer("uid", SHOP_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Apenas o dono");
    }

    @Test
    void shouldThrow403WhenBarberIsNotOwnerForThermometer() {
        UserInfoDTO barber = new UserInfoDTO();
        barber.setId(UUID.randomUUID());
        barber.setUserType("BARBER");
        barber.setRole("ROLE_BARBER");
        barber.setBarbershopId(SHOP_UUID);
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(barber);

        assertThatThrownBy(() -> analyticsService.getAgendaThermometer("uid", SHOP_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Apenas o dono");
    }

    @Test
    void shouldThrow403WhenOwnerAccessesDifferentShopThermometer() {
        UUID otherShop = UUID.randomUUID();
        UserInfoDTO owner = ownerUser(otherShop);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);

        assertThatThrownBy(() -> analyticsService.getAgendaThermometer("owner-uid", SHOP_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Sem permissão");
    }

    // ─── getBarberSkillMatrix(firebaseUid, barbershopId) ──────────────────────

    @Test
    void shouldReturnSkillMatrixForAuthenticatedOwner() {
        UserInfoDTO owner = ownerUser(SHOP_UUID);
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        VBarberSkillMatrix row = skillMatrixRow("b1", "Jo", "Barba", 5L, new BigDecimal("200.00"));
        when(vBarberSkillMatrixRepository.findByBarbershopId(SHOP_ID)).thenReturn(List.of(row));

        List<BarberSkillMatrixResponseDTO> result = analyticsService.getBarberSkillMatrix("owner-uid", SHOP_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).barberId()).isEqualTo("b1");
    }

    @Test
    void shouldThrow403WhenCustomerTriesToAccessSkillMatrix() {
        UserInfoDTO customer = new UserInfoDTO();
        customer.setId(UUID.randomUUID());
        customer.setUserType("CUSTOMER");
        customer.setRole("ROLE_CUSTOMER");
        when(userServiceClient.getUserByFirebaseUid("uid")).thenReturn(customer);

        assertThatThrownBy(() -> analyticsService.getBarberSkillMatrix("uid", SHOP_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Apenas o dono");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private UserInfoDTO ownerUser(UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(UUID.randomUUID());
        user.setUserType("BARBER");
        user.setRole("ROLE_OWNER");
        user.setBarbershopId(shopId);
        return user;
    }

    private VBarberSkillMatrix skillMatrixRow(String barberId, String barberName,
                                               String activityName, Long times, BigDecimal total) {
        VBarberSkillMatrix row = new VBarberSkillMatrix();
        ReflectionTestUtils.setField(row, "barberId", barberId);
        ReflectionTestUtils.setField(row, "barberName", barberName);
        ReflectionTestUtils.setField(row, "activityName", activityName);
        ReflectionTestUtils.setField(row, "timesExecuted", times);
        ReflectionTestUtils.setField(row, "totalGeneratedByActivity", total);
        return row;
    }

    private AgendaThermometerProjection agendaThermometerRow() {
        return new AgendaThermometerProjection() {
            @Override
            public LocalDate getAgendaDate() {
                return LocalDate.of(2026, 5, 16);
            }

            @Override
            public String getBarbershopId() {
                return "shop-1";
            }

            @Override
            public Long getTotalAppointments() {
                return 5L;
            }

            @Override
            public Long getActiveAppointments() {
                return 2L;
            }

            @Override
            public Long getWalkinAppointments() {
                return 1L;
            }

            @Override
            public Long getPendingAppointments() {
                return 0L;
            }

            @Override
            public Long getCompletedAppointments() {
                return 2L;
            }

            @Override
            public Long getLostAppointments() {
                return 0L;
            }
        };
    }
}
