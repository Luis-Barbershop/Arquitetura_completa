package ifsp.edu.projeto.cortaai.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ifsp.edu.projeto.cortaai.schedule.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.schedule.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    private AppointmentDTO appointmentDTO;
    private UUID customerId;
    private UUID barberId;
    private UUID barbershopId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        barberId = UUID.randomUUID();
        barbershopId = UUID.randomUUID();

        appointmentDTO = new AppointmentDTO();
        appointmentDTO.setId(1L);
        appointmentDTO.setBarbershopId(barbershopId);
        appointmentDTO.setBarberId(barberId);
        appointmentDTO.setCustomerId(customerId);
        appointmentDTO.setStatus(AppointmentStatus.SCHEDULED);
        appointmentDTO.setStartTime(OffsetDateTime.now().plusDays(1));
        appointmentDTO.setEndTime(OffsetDateTime.now().plusDays(1).plusMinutes(30));
    }

    @Test
    @DisplayName("GET /api/appointments - Deve listar todos os agendamentos")
    void shouldListAllAppointments() throws Exception {
        when(appointmentService.findAll()).thenReturn(List.of(appointmentDTO));

        mockMvc.perform(get("/api/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/appointments/{id} - Deve buscar agendamento por ID")
    void shouldFindAppointmentById() throws Exception {
        when(appointmentService.findById(1L)).thenReturn(appointmentDTO);

        mockMvc.perform(get("/api/appointments/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/appointments - Deve criar agendamento")
    void shouldCreateAppointment() throws Exception {
        CreateAppointmentDTO createDTO = new CreateAppointmentDTO();
        createDTO.setBarbershopId(barbershopId);
        createDTO.setBarberId(barberId);
        createDTO.setStartTime(OffsetDateTime.now().plusDays(1));
        createDTO.setActivityIds(List.of(UUID.randomUUID()));

        when(appointmentService.create(any(CreateAppointmentDTO.class), eq(customerId)))
                .thenReturn(appointmentDTO);

        mockMvc.perform(post("/api/appointments")
                        .header("X-User-Id", customerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/appointments/{id}/cancel - Deve cancelar agendamento")
    void shouldCancelAppointment() throws Exception {
        mockMvc.perform(patch("/api/appointments/{id}/cancel", 1)
                        .header("X-User-Id", customerId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/appointments/{id} - Deve deletar agendamento")
    void shouldDeleteAppointment() throws Exception {
        mockMvc.perform(delete("/api/appointments/{id}", 1)
                        .header("X-User-Id", customerId.toString()))
                .andExpect(status().isNoContent());
    }
}
