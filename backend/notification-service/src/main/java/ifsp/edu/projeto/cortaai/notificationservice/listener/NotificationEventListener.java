package ifsp.edu.projeto.cortaai.notificationservice.listener;

import ifsp.edu.projeto.cortaai.notificationservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentCancelledEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentConcludedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentCreatedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.PaymentApprovedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;
import ifsp.edu.projeto.cortaai.notificationservice.service.DeduplicationService;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Listeners RabbitMQ para eventos do sistema.
 * Cada listener consome eventos de uma queue específica,
 * faz deduplicação via Redis e cria notificações IN_APP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final DeduplicationService deduplicationService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    /**
     * Agendamento criado → notificar customer + barber.
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_CREATED)
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Evento recebido: appointment.created id={}", event.getAppointmentId());

        String dedupKey = event.getAppointmentId().toString();
        if (deduplicationService.isDuplicate("APPOINTMENT_CREATED", dedupKey)) {
            return;
        }

        String dateStr = event.getStartTime().format(FORMATTER);

        // Notificar o cliente
        notificationService.createNotification(
                event.getCustomerId(),
                NotificationType.APPOINTMENT_CREATED,
                "Agendamento confirmado!",
                String.format("Seu agendamento na %s com %s foi confirmado para %s. Valor: R$ %.2f",
                        event.getBarbershopName(), event.getBarberName(), dateStr, event.getTotalPrice())
        );

        // Notificar o barbeiro
        notificationService.createNotification(
                event.getBarberId(),
                NotificationType.APPOINTMENT_CREATED,
                "Novo agendamento!",
                String.format("Você tem um novo agendamento com %s em %s. Valor: R$ %.2f",
                        event.getCustomerName(), dateStr, event.getTotalPrice())
        );
    }

    /**
     * Agendamento cancelado → notificar a contraparte.
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_CANCELLED)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Evento recebido: appointment.cancelled id={}", event.getAppointmentId());

        String dedupKey = event.getAppointmentId().toString();
        if (deduplicationService.isDuplicate("APPOINTMENT_CANCELLED", dedupKey)) {
            return;
        }

        if ("CUSTOMER".equals(event.getCancelledBy())) {
            // Cliente cancelou → notificar barbeiro
            notificationService.createNotification(
                    event.getBarberId(),
                    NotificationType.APPOINTMENT_CANCELLED,
                    "Agendamento cancelado",
                    "O cliente cancelou o agendamento."
            );
        } else {
            // Barbeiro cancelou → notificar cliente
            notificationService.createNotification(
                    event.getCustomerId(),
                    NotificationType.APPOINTMENT_CANCELLED,
                    "Agendamento cancelado",
                    "O barbeiro cancelou o seu agendamento. Tente agendar novamente."
            );
        }
    }

    /**
     * Agendamento concluído → pedir avaliação ao customer.
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_CONCLUDED)
    public void onAppointmentConcluded(AppointmentConcludedEvent event) {
        log.info("Evento recebido: appointment.concluded id={}", event.getAppointmentId());

        String dedupKey = event.getAppointmentId().toString();
        if (deduplicationService.isDuplicate("APPOINTMENT_CONCLUDED", dedupKey)) {
            return;
        }

        // Notificar o cliente para avaliar
        notificationService.createNotification(
                event.getCustomerId(),
                NotificationType.APPOINTMENT_CONCLUDED,
                "Atendimento concluído!",
                "Seu atendimento foi concluído. Que tal deixar uma avaliação?"
        );
    }

    /**
     * Pagamento aprovado → confirmar pagamento ao customer.
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_PAYMENT_APPROVED)
    public void onPaymentApproved(PaymentApprovedEvent event) {
        log.info("Evento recebido: payment.approved txId={}", event.getTransactionId());

        String dedupKey = event.getTransactionId().toString();
        if (deduplicationService.isDuplicate("PAYMENT_APPROVED", dedupKey)) {
            return;
        }

        notificationService.createNotification(
                event.getCustomerId(),
                NotificationType.PAYMENT_APPROVED,
                "Pagamento aprovado!",
                String.format("Seu pagamento de R$ %.2f foi aprovado com sucesso.", event.getAmount())
        );
    }
}
