package ifsp.edu.projeto.cortaai.notificationservice.listener;

import ifsp.edu.projeto.cortaai.notificationservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentCancelledEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentConcludedEvent;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentCreatedEvent;
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

    @RabbitListener(queues = RabbitConfig.QUEUE_PAYMENT_APPROVED)
    public void onPaymentApproved(PaymentApprovedEvent event) {
        log.info("Evento recebido: payment.approved txId={}", event.getTransactionId());
        if (deduplicationService.isDuplicate("PAYMENT_APPROVED", event.getTransactionId().toString())) return;

        notificationService.notifyPaymentApproved(
                event.getCustomerId(), event.getCustomerEmail(), event.getAmount());
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_JOIN_REQUEST_CREATED)
    public void onJoinRequestCreated(JoinRequestCreatedEvent event) {
        log.info("Evento recebido: barbershop.join-request.created requestId={} barberId={} ownerId={}",
                event.getRequestId(), event.getBarberId(), event.getOwnerId());
        if (deduplicationService.isDuplicate("JOIN_REQUEST_CREATED", event.getRequestId().toString())) return;

        notificationService.notifyJoinRequestReceived(
                event.getOwnerId(), event.getBarbershopName(), event.getBarberName());
    }
}
