package ifsp.edu.projeto.cortaai.scheduleservice.mapper;

import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.AppointmentActivity;
import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMapperTest {

    private final AppointmentMapper mapper = new AppointmentMapperImpl();

    @Test
    void shouldMapAppointmentAndResolveLegacyStatuses() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .barberId(UUID.randomUUID())
                .barbershopId(UUID.randomUUID())
                .customerName("Cliente")
                .barberName("Barbeiro")
                .barbershopName("Barbearia")
                .startTime(LocalDateTime.of(2026, 5, 22, 9, 0))
                .endTime(LocalDateTime.of(2026, 5, 22, 10, 0))
                .totalPrice(new BigDecimal("50.00"))
                .status(AppointmentStatus.CONCLUDED)
                .dateCreated(LocalDateTime.now())
                .activities(Set.of(activity("Corte")))
                .build();

        var dto = mapper.toDTO(appointment);

        assertThat(dto.getId()).isEqualTo(appointment.getId());
        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getActivities()).hasSize(1);
        assertThat(dto.getActivities().get(0).getActivityName()).isEqualTo("Corte");
        assertThat(mapper.mapStatus(null)).isNull();
    }

    @Test
    void shouldProjectExpiredPaymentPendingAppointments() {
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .status(AppointmentStatus.PAYMENT_PENDING)
                .dateCreated(LocalDateTime.now().minusMinutes(31))
                .activities(Set.of())
                .build();

        assertThat(mapper.toDTO(appointment).getStatus()).isEqualTo("EXPIRED");
    }

    @Test
    void shouldMapActivitiesAndBlocks() {
        AppointmentActivity activity = activity("Barba");
        BarberBlock block = BarberBlock.builder()
                .id(UUID.randomUUID())
                .barberId(UUID.randomUUID())
                .startTime(LocalDateTime.of(2026, 5, 22, 12, 0))
                .endTime(LocalDateTime.of(2026, 5, 22, 13, 0))
                .reason("Almoco")
                .dateCreated(LocalDateTime.of(2026, 5, 21, 8, 0))
                .build();

        assertThat(mapper.toActivityDTO(activity).getActivityName()).isEqualTo("Barba");
        assertThat(mapper.toActivityDTOList(Set.of(activity))).hasSize(1);
        assertThat(mapper.toBlockDTO(block).getReason()).isEqualTo("Almoco");
        assertThat(mapper.toBlockDTOList(List.of(block))).hasSize(1);
    }

    private static AppointmentActivity activity(String name) {
        return AppointmentActivity.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .activityName(name)
                .price(BigDecimal.TEN)
                .durationMinutes(30)
                .build();
    }
}
