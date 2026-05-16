package ifsp.edu.projeto.cortaai.notificationservice.service;

import ifsp.edu.projeto.cortaai.notificationservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.model.Notification;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;
import ifsp.edu.projeto.cortaai.notificationservice.repository.NotificationRepository;
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
        when(notificationRepository.save(any(Notification.class)))
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

        notificationService.notifyInviteReceived(barberId, "Barbearia Convite");

        verify(pushNotificationService)
                .sendToUser(
                        eq(barberId),
                        eq("Você recebeu um convite!"),
                        contains("Barbearia Convite"),
                        argThat(data -> NotificationType.INVITE_RECEIVED.name().equals(data.get("type"))
                                && "/barberProfile".equals(data.get("deepLink")))
                );
    }
}
