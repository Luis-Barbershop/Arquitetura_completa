package ifsp.edu.projeto.cortaai.notificationservice.service;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentReminderEvent;
import ifsp.edu.projeto.cortaai.notificationservice.feign.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.model.Notification;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationChannel;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;
import ifsp.edu.projeto.cortaai.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PushNotificationService pushNotificationService;
    @Mock
    private ScheduleServiceClient scheduleServiceClient;
    @Mock
    private SseEmitterService sseEmitterService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, emailService, pushNotificationService, scheduleServiceClient, sseEmitterService);
        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldSendPushForPaymentApproved() {
        UUID customerId = UUID.randomUUID();
        notificationService.notifyPaymentApproved(customerId, "cliente@cortaai.com", new BigDecimal("49.90"), null);

        verify(pushNotificationService, times(1))
                .sendToUser(eq(customerId), eq("Pagamento aprovado!"),
                        argThat(body -> body.contains("49") && body.contains("90")), anyMap());
    }

    @Test
    void shouldSendCreatedNotificationThroughSse() {
        UUID userId = UUID.randomUUID();
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(4L);

        Notification saved = notificationService.createNotification(
                userId,
                NotificationType.APPOINTMENT_CREATED,
                "Novo agendamento",
                "Mensagem"
        );

        verify(sseEmitterService).sendUnreadCount(userId, 4L);
        verify(sseEmitterService).sendNotificationCreated(eq(userId), any(NotificationDTO.class), eq(4L));
        assertThat(saved.getTitle()).isEqualTo("Novo agendamento");
    }

    @Test
    void shouldSendPushForBothSidesOnAppointmentCreated() {
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        notificationService.notifyAppointmentCreated(
                customerId,
                "customer@cortaai.com",
                "Cliente",
                barberId,
                "barber@cortaai.com",
                "Barbeiro",
                "Barbearia XPTO",
                LocalDateTime.of(2026, 4, 27, 16, 0),
                new BigDecimal("75.00")
        );

        verify(pushNotificationService, times(1))
                .sendToUser(eq(customerId), eq("Agendamento confirmado!"), contains("Barbearia XPTO"), anyMap());
        verify(pushNotificationService, times(1))
                .sendToUser(eq(barberId), eq("Novo agendamento!"), contains("Cliente"), anyMap());
    }

    @Test
    void shouldIncludeDeepLinkAndTypeInPushDataForInvite() {
        UUID barberId = UUID.randomUUID();

        notificationService.notifyInviteReceived(barberId, "barber@cortaai.com", "Barbearia Convite");

        verify(pushNotificationService)
                .sendToUser(
                        eq(barberId),
                        eq("Você recebeu um convite!"),
                        contains("Barbearia Convite"),
                        argThat(data -> NotificationType.INVITE_RECEIVED.name().equals(data.get("type"))
                                && "/barberHome/perfil".equals(data.get("deepLink")))
                );
    }

    @Test
    void shouldNotifyBarberWhenCustomerCancelsAppointment() {
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        notificationService.notifyAppointmentCancelled(
                customerId,
                "customer@cortaai.com",
                "Cliente",
                barberId,
                "barber@cortaai.com",
                "Barbeiro",
                "Barbearia",
                LocalDateTime.of(2026, 5, 21, 14, 0),
                "CUSTOMER"
        );

        verify(pushNotificationService).sendToUser(eq(barberId), eq("Agendamento cancelado"), contains("Cliente"), anyMap());
        verify(emailService).sendCancelledByCustomerToBarber(
                eq("barber@cortaai.com"),
                eq("Barbeiro"),
                eq("Cliente"),
                eq(LocalDateTime.of(2026, 5, 21, 14, 0)));
        verify(pushNotificationService, never()).sendToUser(eq(customerId), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldNotifyCustomerWhenBarberCancelsAppointment() {
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        notificationService.notifyAppointmentCancelled(
                customerId,
                "customer@cortaai.com",
                "Cliente",
                barberId,
                "barber@cortaai.com",
                "Barbeiro",
                "Barbearia",
                LocalDateTime.of(2026, 5, 21, 14, 0),
                "BARBER"
        );

        verify(pushNotificationService).sendToUser(eq(customerId), eq("Agendamento cancelado"), contains("barbeiro"), anyMap());
        verify(emailService).sendCancelledByBarberToCustomer(
                eq("customer@cortaai.com"),
                eq("Cliente"),
                eq("Barbearia"),
                eq("Barbeiro"),
                eq(LocalDateTime.of(2026, 5, 21, 14, 0)));
        verify(pushNotificationService, never()).sendToUser(eq(barberId), anyString(), anyString(), anyMap());
    }

    @Test
    void shouldNotifyBothSidesWhenAppointmentIsRescheduled() {
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();

        notificationService.notifyAppointmentRescheduled(
                customerId,
                "customer@cortaai.com",
                "Cliente",
                barberId,
                "barber@cortaai.com",
                "Barbeiro",
                "Barbearia",
                LocalDateTime.of(2026, 5, 21, 14, 0),
                LocalDateTime.of(2026, 5, 22, 15, 0)
        );

        verify(pushNotificationService).sendToUser(eq(customerId), eq("Agendamento reagendado"), contains("22/05/2026"), anyMap());
        verify(pushNotificationService).sendToUser(eq(barberId), eq("Agendamento reagendado"), contains("Cliente"), anyMap());
        verify(emailService).sendRescheduledToCustomer(anyString(), eq("Cliente"), eq("Barbearia"), eq("Barbeiro"), any(), any());
        verify(emailService).sendRescheduledToBarber(anyString(), eq("Barbeiro"), eq("Cliente"), any(), any());
    }

    @Test
    void shouldNotifyCustomerWhenAppointmentIsConcluded() {
        UUID customerId = UUID.randomUUID();

        notificationService.notifyAppointmentConcluded(
                customerId,
                "customer@cortaai.com",
                "Cliente",
                "Barbeiro",
                "Barbearia"
        );

        verify(pushNotificationService).sendToUser(
                eq(customerId),
                eq("Atendimento concluído!"),
                contains("avaliação"),
                argThat(data -> NotificationType.APPOINTMENT_CONCLUDED.name().equals(data.get("type"))
                        && "/meus-agendamentos".equals(data.get("deepLink"))));
        verify(emailService).sendConcludedToCustomer(
                "customer@cortaai.com",
                "Cliente",
                "Barbeiro",
                "Barbearia");
    }

    @Test
    void shouldNotifyOwnerWhenBarberRequestsToJoinBarbershop() {
        UUID ownerId = UUID.randomUUID();

        notificationService.notifyJoinRequestReceived(
                ownerId,
                "owner@cortaai.com",
                "Barbearia",
                "Barbeiro"
        );

        verify(pushNotificationService).sendToUser(
                eq(ownerId),
                eq("Novo pedido de entrada!"),
                contains("Barbeiro"),
                argThat(data -> NotificationType.JOIN_REQUEST_RECEIVED.name().equals(data.get("type"))
                        && "/barberHome/time".equals(data.get("deepLink"))));
        verify(emailService).sendJoinRequestReceivedToOwner("owner@cortaai.com", "Barbearia", "Barbeiro");
    }

    @Test
    void shouldNotifyBarberWhenRemovedFromBarbershop() {
        UUID barberId = UUID.randomUUID();

        notificationService.notifyBarberRemoved(
                barberId,
                "barber@cortaai.com",
                "Barbeiro",
                "Barbearia"
        );

        verify(pushNotificationService).sendToUser(
                eq(barberId),
                eq("Você foi removido da barbearia"),
                contains("Barbearia"),
                argThat(data -> NotificationType.BARBER_REMOVED.name().equals(data.get("type"))
                        && "/barberHome/perfil".equals(data.get("deepLink"))));
        verify(emailService).sendBarberRemovedToBarber("barber@cortaai.com", "Barbeiro", "Barbearia");
    }

    @Test
    void shouldCreateReminderNotificationAndEmailWhenCustomerEmailIsPresent() {
        AppointmentReminderEvent event = new AppointmentReminderEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cliente",
                "customer@cortaai.com",
                UUID.randomUUID(),
                "barber@cortaai.com",
                "Barbearia",
                "Barbeiro",
                LocalDateTime.of(2026, 5, 21, 16, 30)
        );

        notificationService.notifyAppointmentReminder(event);

        verify(pushNotificationService).sendToUser(eq(event.getCustomerId()), eq("Seu horário está chegando!"), contains("16:30"), anyMap());
        verify(emailService).sendReminderToCustomer(
                eq("customer@cortaai.com"),
                eq("Cliente"),
                eq("Barbearia"),
                eq("Barbeiro"),
                eq(LocalDateTime.of(2026, 5, 21, 16, 30)));
    }

    @Test
    void shouldCreateReminderNotificationForBarberWhenBarberIdIsPresent() {
        AppointmentReminderEvent event = new AppointmentReminderEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Cliente",
                "",
                UUID.randomUUID(),
                "barber@cortaai.com",
                "Barbearia",
                "Barbeiro",
                LocalDateTime.of(2026, 5, 21, 16, 30)
        );

        notificationService.notifyAppointmentReminder(event);

        verify(pushNotificationService).sendToUser(
                eq(event.getBarberId()),
                eq("Lembrete de atendimento"),
                contains("Cliente"),
                argThat(data -> NotificationType.APPOINTMENT_REMINDER.name().equals(data.get("type"))
                        && "/barberHome".equals(data.get("deepLink"))));
        verify(emailService, never()).sendReminderToCustomer(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void shouldNotifyBarberAboutPaymentWhenAppointmentCanBeLoaded() {
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        AppointmentInfoDTO appointment = new AppointmentInfoDTO();
        appointment.setBarberId(barberId);
        appointment.setBarbershopName("Barbearia");

        when(scheduleServiceClient.getAppointmentById(appointmentId)).thenReturn(appointment);

        notificationService.notifyPaymentApproved(customerId, "", new BigDecimal("80.00"), appointmentId);

        verify(pushNotificationService).sendToUser(eq(customerId), eq("Pagamento aprovado!"), contains("80"), anyMap());
        verify(pushNotificationService).sendToUser(eq(barberId), eq("Pagamento recebido!"), contains("Barbearia"), anyMap());
    }

    @Test
    void shouldTolerateFailureWhenLoadingAppointmentForPaymentNotification() {
        UUID appointmentId = UUID.randomUUID();
        when(scheduleServiceClient.getAppointmentById(appointmentId)).thenThrow(new RuntimeException("schedule offline"));

        notificationService.notifyPaymentApproved(UUID.randomUUID(), "", new BigDecimal("80.00"), appointmentId);

        verify(pushNotificationService, times(1)).sendToUser(any(UUID.class), eq("Pagamento aprovado!"), anyString(), anyMap());
    }

    @Test
    void shouldReturnNotificationsMappedToDto() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.INVITE_RECEIVED)
                .title("Convite")
                .message("Mensagem")
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .createdAt(LocalDateTime.of(2026, 5, 21, 10, 0))
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        List<NotificationDTO> result = notificationService.getMyNotifications(userId);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(notificationId);
            assertThat(dto.userId()).isEqualTo(userId);
            assertThat(dto.type()).isEqualTo(NotificationType.INVITE_RECEIVED);
            assertThat(dto.read()).isFalse();
        });
    }

    @Test
    void shouldMarkNotificationAsReadAndSendUnreadCount() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.APPOINTMENT_CREATED)
                .title("Título")
                .message("Mensagem")
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .createdAt(LocalDateTime.of(2026, 5, 21, 10, 0))
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(2L);

        NotificationDTO result = notificationService.markAsRead(notificationId, userId);

        assertThat(notification.isRead()).isTrue();
        assertThat(result.read()).isTrue();
        verify(sseEmitterService).sendUnreadCount(userId, 2L);
    }

    @Test
    void shouldRejectMarkAsReadForNotificationOwnedByAnotherUser() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(UUID.randomUUID())
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Notificação não pertence ao usuário");

        verify(notificationRepository, never()).save(any());
    }
}
