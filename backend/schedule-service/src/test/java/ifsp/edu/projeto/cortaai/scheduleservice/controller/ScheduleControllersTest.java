package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;
import ifsp.edu.projeto.cortaai.scheduleservice.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleControllersTest {

    @Mock
    private AppointmentService appointmentService;
    @Mock
    private BarberBlockService barberBlockService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private AiChatService aiChatService;
    @Mock
    private ChatHistoryService chatHistoryService;

    private AppointmentController appointmentController;
    private InternalAppointmentController internalAppointmentController;
    private BarberBlockController barberBlockController;
    private AnalyticsController analyticsController;
    private AiChatController aiChatController;

    @BeforeEach
    void setUp() {
        appointmentController = new AppointmentController(appointmentService);
        internalAppointmentController = new InternalAppointmentController(appointmentService);
        barberBlockController = new BarberBlockController(barberBlockService);
        analyticsController = new AnalyticsController(analyticsService);
        aiChatController = new AiChatController(aiChatService, chatHistoryService);
    }

    @Test
    void shouldDelegateAppointmentEndpoints() {
        UUID id = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 22);
        AppointmentDTO appointment = appointmentDto(id);
        CreateAppointmentDTO create = new CreateAppointmentDTO();
        RescheduleAppointmentDTO reschedule = new RescheduleAppointmentDTO(LocalDateTime.of(2026, 5, 23, 10, 0), barberId);
        BarberManualBookingDTO manualBooking = new BarberManualBookingDTO();

        when(appointmentService.createAppointment("user@example.com", create)).thenReturn(appointment);
        when(appointmentService.getAppointmentById(id)).thenReturn(appointment);
        when(appointmentService.getMyAppointments("user@example.com")).thenReturn(List.of(appointment));
        when(appointmentService.getBarberSchedule(barberId, date)).thenReturn(List.of(appointment));
        when(appointmentService.getBarbershopSchedule(shopId, date, null, null, "user@example.com", "cid-1")).thenReturn(List.of(appointment));
        when(appointmentService.getAvailability(barberId, date, 45)).thenReturn(List.of(new TimeSlotDTO(LocalDateTime.now(), LocalDateTime.now().plusMinutes(45), true)));
        when(appointmentService.createManualBooking("firebase-uid", manualBooking)).thenReturn(appointment);

        assertThat(appointmentController.createAppointment("user@example.com", create).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(appointmentController.getAppointment(id).getBody()).isEqualTo(appointment);
        assertThat(appointmentController.getMyAppointments("user@example.com").getBody()).containsExactly(appointment);
        assertThat(appointmentController.getBarberSchedule(barberId, date).getBody()).containsExactly(appointment);
        assertThat(appointmentController.getBarbershopSchedule("user@example.com", "cid-1", shopId, date, null, null).getBody()).containsExactly(appointment);
        assertThat(appointmentController.cancelAppointment("user@example.com", id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(appointmentController.rescheduleAppointment("user@example.com", id, reschedule).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(appointmentController.concludeAppointment("user@example.com", id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(appointmentController.completeAppointment("user@example.com", id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(appointmentController.confirmAppointment("user@example.com", id).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(appointmentController.getAvailability(barberId, date, 45).getBody()).hasSize(1);
        assertThat(appointmentController.createManualBooking("firebase-uid", manualBooking).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        verify(appointmentService).cancelAppointment("user@example.com", id);
        verify(appointmentService).rescheduleAppointment("user@example.com", id, reschedule);
        verify(appointmentService, times(2)).concludeAppointment("user@example.com", id);
        verify(appointmentService).confirmAppointment("user@example.com", id);
    }

    @Test
    void shouldDelegateInternalAppointmentEndpoints() {
        UUID id = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 5, 22, 9, 0);
        LocalDateTime to = from.plusHours(8);
        AppointmentDTO appointment = appointmentDto(id);

        when(appointmentService.getAppointmentById(id)).thenReturn(appointment);
        when(appointmentService.getBarbershopAppointmentsByPeriod(shopId, from, to)).thenReturn(List.of(appointment));
        when(appointmentService.getFutureAppointmentsByBarber(barberId)).thenReturn(List.of(appointment));

        assertThat(internalAppointmentController.getAppointmentById(id).getBody()).isEqualTo(appointment);
        assertThat(internalAppointmentController.getBarbershopAppointmentsByPeriod(shopId, from, to).getBody()).containsExactly(appointment);
        assertThat(internalAppointmentController.getFutureAppointmentsByBarber(barberId).getBody()).containsExactly(appointment);
        assertThat(internalAppointmentController.updatePaymentStatus(id, null, "PAID").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(internalAppointmentController.updatePaymentStatus(id, "FAILED", "IGNORED").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(appointmentService).updatePaymentStatus(id, "PAID");
        verify(appointmentService).updatePaymentStatus(id, "FAILED");
    }

    @Test
    void shouldDelegateBarberBlockAnalyticsAndAiEndpoints() {
        UUID barberId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 22);
        CreateBarberBlockDTO createBlock = new CreateBarberBlockDTO();
        BarberBlockDTO block = new BarberBlockDTO();
        AiChatRequestDTO chatRequest = new AiChatRequestDTO("Como esta a agenda?", AiChatMode.PREVIEW);
        AiChatResponseDTO chatResponse = new AiChatResponseDTO("Agenda tranquila", "fallback", AiChatMode.PREVIEW);
        AgendaThermometerResponseDTO thermometer = new AgendaThermometerResponseDTO(date, "shop-1", 1L, 1L, 0L, 0L, 0L, 0L);
        BarberSkillMatrixResponseDTO skill = new BarberSkillMatrixResponseDTO("barber-1", "Barbeiro", "Corte", 3L, java.math.BigDecimal.TEN);

        when(barberBlockService.createBlock("firebase-uid", createBlock)).thenReturn(block);
        when(barberBlockService.getBlocks(barberId, date)).thenReturn(List.of(block));
        when(analyticsService.getAgendaThermometer("firebase-uid", "shop-1")).thenReturn(List.of(thermometer));
        when(analyticsService.getBarberSkillMatrix("firebase-uid", "shop-1")).thenReturn(List.of(skill));
        when(aiChatService.chat("firebase-uid", "OWNER", chatRequest)).thenReturn(chatResponse);

        assertThat(barberBlockController.createBlock("firebase-uid", createBlock).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(barberBlockController.getBlocks(barberId, date).getBody()).containsExactly(block);
        assertThat(barberBlockController.deleteBlock("firebase-uid", blockId).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(analyticsController.getAgendaThermometer("firebase-uid", "shop-1").getBody()).containsExactly(thermometer);
        assertThat(analyticsController.getBarberSkillMatrix("firebase-uid", "shop-1").getBody()).containsExactly(skill);
        assertThat(aiChatController.chat("firebase-uid", "OWNER", chatRequest).getBody()).isEqualTo(chatResponse);
        assertThat(aiChatController.clearHistory("firebase-uid").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(barberBlockService).deleteBlock("firebase-uid", blockId);
        verify(chatHistoryService).clearHistory("firebase-uid");
    }

    private static AppointmentDTO appointmentDto(UUID id) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(id);
        dto.setStatus("CONFIRMED");
        return dto;
    }
}
