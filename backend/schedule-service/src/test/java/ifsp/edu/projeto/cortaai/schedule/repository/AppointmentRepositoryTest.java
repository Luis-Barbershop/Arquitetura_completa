package ifsp.edu.projeto.cortaai.schedule.repository;

import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AppointmentRepositoryTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("Deve salvar um agendamento com sucesso")
    void shouldSaveAppointment() {
        // given
        Appointment appointment = new Appointment();
        appointment.setBarbershopId(UUID.randomUUID());
        appointment.setBarberId(UUID.randomUUID());
        appointment.setCustomerId(UUID.randomUUID());
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        // when
        Appointment saved = appointmentRepository.save(appointment);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Deve encontrar agendamentos por barbeiro")
    void shouldFindByBarberId() {
        // given
        UUID barberId = UUID.randomUUID();
        
        Appointment appointment = new Appointment();
        appointment.setBarbershopId(UUID.randomUUID());
        appointment.setBarberId(barberId);
        appointment.setCustomerId(UUID.randomUUID());
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        // when
        List<Appointment> found = appointmentRepository.findByBarberId(barberId);

        // then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getBarberId()).isEqualTo(barberId);
    }

    @Test
    @DisplayName("Deve encontrar agendamentos por cliente")
    void shouldFindByCustomerId() {
        // given
        UUID customerId = UUID.randomUUID();
        
        Appointment appointment = new Appointment();
        appointment.setBarbershopId(UUID.randomUUID());
        appointment.setBarberId(UUID.randomUUID());
        appointment.setCustomerId(customerId);
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        // when
        List<Appointment> found = appointmentRepository.findByCustomerId(customerId);

        // then
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Deve encontrar agendamentos conflitantes")
    void shouldFindConflictingAppointments() {
        // given
        UUID barberId = UUID.randomUUID();
        OffsetDateTime startTime = OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0);
        OffsetDateTime endTime = startTime.plusMinutes(30);
        
        Appointment appointment = new Appointment();
        appointment.setBarbershopId(UUID.randomUUID());
        appointment.setBarberId(barberId);
        appointment.setCustomerId(UUID.randomUUID());
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        // when - trying to find conflict with overlapping time
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                barberId, startTime.plusMinutes(15), endTime.plusMinutes(15));

        // then
        assertThat(conflicts).hasSize(1);
    }
}
