package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AgendaThermometerResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VBarberSkillMatrixRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.projection.AgendaThermometerProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private VBarberSkillMatrixRepository vBarberSkillMatrixRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

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
