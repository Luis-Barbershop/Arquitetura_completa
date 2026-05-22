package ifsp.edu.projeto.cortaai.notificationservice.listener;

import ifsp.edu.projeto.cortaai.notificationservice.event.*;
import ifsp.edu.projeto.cortaai.notificationservice.service.DeduplicationService;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private DeduplicationService deduplicationService;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationService, deduplicationService);
    }

    @Test
    void shouldDispatchSupportedEventsWhenTheyAreNotDuplicates() {
        when(deduplicationService.isDuplicate(anyString(), anyString())).thenReturn(false);
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 5, 22, 10, 0);

        listener.onAppointmentCreated(new AppointmentCreatedEvent(
                appointmentId, customerId, barberId, UUID.randomUUID(), "Cliente", "cliente@example.com",
                "Barbeiro", "barber@example.com", "Barbearia", start, new BigDecimal("80.00")));
        listener.onAppointmentCancelled(new AppointmentCancelledEvent(
                appointmentId, customerId, barberId, "CUSTOMER", "Cliente", "cliente@example.com",
                "Barbeiro", "barber@example.com", "Barbearia", start));
        listener.onAppointmentConcluded(new AppointmentConcludedEvent(
                appointmentId, customerId, barberId, UUID.randomUUID(), "Cliente", "cliente@example.com",
                "Barbeiro", "Barbearia", start));
        listener.onAppointmentRescheduled(new AppointmentRescheduledEvent(
                appointmentId, customerId, barberId, UUID.randomUUID(), "Cliente", "cliente@example.com",
                "Barbeiro", "barber@example.com", "Barbearia", start, start.plusHours(1),
                start.plusHours(2), "CUSTOMER"));
        listener.onPaymentApproved(new PaymentApprovedEvent(
                UUID.randomUUID(), appointmentId, customerId, "cliente@example.com", new BigDecimal("80.00")));
        listener.onAppointmentReminder(new AppointmentReminderEvent(
                appointmentId, customerId, "Cliente", "cliente@example.com", "Barbearia", "Barbeiro", start));

        verify(notificationService).notifyAppointmentCreated(
                eq(customerId), eq("cliente@example.com"), eq("Cliente"),
                eq(barberId), eq("barber@example.com"), eq("Barbeiro"),
                eq("Barbearia"), eq(start), eq(new BigDecimal("80.00")));
        verify(notificationService).notifyAppointmentCancelled(
                eq(customerId), eq("cliente@example.com"), eq("Cliente"),
                eq(barberId), eq("barber@example.com"), eq("Barbeiro"),
                eq("Barbearia"), eq(start), eq("CUSTOMER"));
        verify(notificationService).notifyAppointmentConcluded(
                eq(customerId), eq("cliente@example.com"), eq("Cliente"), eq("Barbeiro"), eq("Barbearia"));
        verify(notificationService).notifyAppointmentRescheduled(
                eq(customerId), eq("cliente@example.com"), eq("Cliente"),
                eq(barberId), eq("barber@example.com"), eq("Barbeiro"),
                eq("Barbearia"), eq(start), eq(start.plusHours(1)));
        verify(notificationService).notifyPaymentApproved(customerId, "cliente@example.com", new BigDecimal("80.00"), appointmentId);
        verify(notificationService).notifyAppointmentReminder(any(AppointmentReminderEvent.class));
    }

    @Test
    void shouldDispatchJoinRequestByRequestType() {
        when(deduplicationService.isDuplicate(eq("JOIN_REQUEST_CREATED"), anyString())).thenReturn(false);
        UUID barberId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        JoinRequestCreatedEvent invite = joinRequest("INVITE", barberId, ownerId);
        JoinRequestCreatedEvent join = joinRequest("JOIN", barberId, ownerId);

        listener.onJoinRequestCreated(invite);
        listener.onJoinRequestCreated(join);

        verify(notificationService).notifyInviteReceived(barberId, "Barbearia");
        verify(notificationService).notifyJoinRequestReceived(ownerId, "Barbearia", "Barbeiro");
    }

    @Test
    void shouldIgnoreDuplicateEvents() {
        when(deduplicationService.isDuplicate(eq("APPOINTMENT_CREATED"), anyString())).thenReturn(true);

        listener.onAppointmentCreated(new AppointmentCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Cliente", "cliente@example.com", "Barbeiro", "barber@example.com",
                "Barbearia", LocalDateTime.now(), BigDecimal.TEN));

        verifyNoInteractions(notificationService);
    }

    private static JoinRequestCreatedEvent joinRequest(String type, UUID barberId, UUID ownerId) {
        JoinRequestCreatedEvent event = new JoinRequestCreatedEvent();
        event.setRequestId(UUID.randomUUID());
        event.setBarberId(barberId);
        event.setOwnerId(ownerId);
        event.setBarberName("Barbeiro");
        event.setBarbershopName("Barbearia");
        event.setRequestType(type);
        return event;
    }
}
