package ifsp.edu.projeto.cortaai.notificationservice.listener;

import ifsp.edu.projeto.cortaai.notificationservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentCancelledEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentConcludedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentCreatedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentRescheduledEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentReminderEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.JoinRequestCreatedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.PaymentApprovedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.service.DeduplicationService;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listeners RabbitMQ para eventos do sistema.
 * Cada listener faz deduplicação via Redis, cria notificação IN_APP
 * e dispara o e-mail transacional correspondente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final DeduplicationService deduplicationService;

    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_CREATED)
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Evento recebido: appointment.created id={}", event.getAppointmentId());
        if (deduplicationService.isDuplicate("APPOINTMENT_CREATED", event.getAppointmentId().toString())) return;

        notificationService.notifyAppointmentCreated(
                event.getCustomerId(), event.getCustomerEmail(), event.getCustomerName(),
                event.getBarberId(), event.getBarberEmail(), event.getBarberName(),
                event.getBarbershopName(), event.getStartTime(), event.getTotalPrice());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_CANCELLED)
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("Evento recebido: appointment.cancelled id={}", event.getAppointmentId());
        if (deduplicationService.isDuplicate("APPOINTMENT_CANCELLED", event.getAppointmentId().toString())) return;

        notificationService.notifyAppointmentCancelled(
                event.getCustomerId(), event.getCustomerEmail(), event.getCustomerName(),
                event.getBarberId(), event.getBarberEmail(), event.getBarberName(),
                event.getBarbershopName(), event.getStartTime(), event.getCancelledBy());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_CONCLUDED)
    public void onAppointmentConcluded(AppointmentConcludedEvent event) {
        log.info("Evento recebido: appointment.concluded id={}", event.getAppointmentId());
        if (deduplicationService.isDuplicate("APPOINTMENT_CONCLUDED", event.getAppointmentId().toString())) return;

        notificationService.notifyAppointmentConcluded(
                event.getCustomerId(), event.getCustomerEmail(), event.getCustomerName(),
                event.getBarberName(), event.getBarbershopName());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_RESCHEDULED)
    public void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
        log.info("Evento recebido: appointment.rescheduled id={}", event.getAppointmentId());
        if (deduplicationService.isDuplicate("APPOINTMENT_RESCHEDULED", event.getAppointmentId().toString())) return;

        notificationService.notifyAppointmentRescheduled(
                event.getCustomerId(), event.getCustomerEmail(), event.getCustomerName(),
                event.getBarberId(), event.getBarberEmail(), event.getBarberName(),
                event.getBarbershopName(), event.getOldStartTime(), event.getNewStartTime());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_PAYMENT_APPROVED)
    public void onPaymentApproved(PaymentApprovedEvent event) {
        log.info("Evento recebido: payment.approved txId={}", event.getTransactionId());
        if (deduplicationService.isDuplicate("PAYMENT_APPROVED", event.getTransactionId().toString())) return;

        notificationService.notifyPaymentApproved(
                event.getCustomerId(), event.getCustomerEmail(), event.getAmount(), event.getAppointmentId());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_JOIN_REQUEST_CREATED)
    public void onJoinRequestCreated(JoinRequestCreatedEvent event) {
        log.info("Evento recebido: barbershop.join-request.created requestId={} barberId={} ownerId={} type={}",
                event.getRequestId(), event.getBarberId(), event.getOwnerId(), event.getRequestType());
        if (deduplicationService.isDuplicate("JOIN_REQUEST_CREATED", event.getRequestId().toString())) return;

        if ("INVITE".equalsIgnoreCase(event.getRequestType())) {
            // Owner convidou barbeiro → notificar o barbeiro
            notificationService.notifyInviteReceived(event.getBarberId(), event.getBarbershopName());
        } else {
            // Barbeiro pediu entrada → notificar o dono
            notificationService.notifyJoinRequestReceived(
                    event.getOwnerId(), event.getBarbershopName(), event.getBarberName());
        }
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_APPOINTMENT_REMINDER)
    public void onAppointmentReminder(AppointmentReminderEvent event) {
        log.info("Evento recebido: appointment.reminder appointmentId={}", event.getAppointmentId());
        if (deduplicationService.isDuplicate("APPOINTMENT_REMINDER", event.getAppointmentId().toString())) return;
        notificationService.notifyAppointmentReminder(event);
    }
}
