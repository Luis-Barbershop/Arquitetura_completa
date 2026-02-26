package ifsp.edu.projeto.cortaai.schedule.integration;

import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.schedule.repository.AppointmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("GET /api/appointments - Integração: Lista agendamentos")
    void shouldListAppointmentsIntegration() throws Exception {
        // given
        appointmentRepository.deleteAll();

        Appointment appointment = new Appointment();
        appointment.setBarbershopId(UUID.randomUUID());
        appointment.setBarberId(UUID.randomUUID());
        appointment.setCustomerId(UUID.randomUUID());
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        // when/then
        mockMvc.perform(get("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} - Integração: Busca por ID")
    void shouldFindByIdIntegration() throws Exception {
        // given
        Appointment appointment = new Appointment();
        appointment.setBarbershopId(UUID.randomUUID());
        appointment.setBarberId(UUID.randomUUID());
        appointment.setCustomerId(UUID.randomUUID());
        appointment.setStartTime(OffsetDateTime.now().plusDays(1));
        appointment.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment = appointmentRepository.save(appointment);

        // when/then
        mockMvc.perform(get("/api/appointments/{id}", appointment.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointment.getId()));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} - Integração: 404 quando não encontrado")
    void shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/appointments/{id}", 99999)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
