package ifsp.edu.projeto.cortaai.notificationservice.service;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.event.AppointmentReminderEvent;
import ifsp.edu.projeto.cortaai.notificationservice.feign.AppointmentInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.model.Notification;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationChannel;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;
import ifsp.edu.projeto.cortaai.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de notificações — orquestra a criação IN_APP e o disparo de e-mail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;
    private final ScheduleServiceClient scheduleServiceClient;
    private final SseEmitterService sseEmitterService;

    // ─── Agendamento criado ──────────────────────────────────────────────────────

    @Transactional
    public void notifyAppointmentCreated(
            UUID customerId, String customerEmail, String customerName,
            UUID barberId, String barberEmail, String barberName,
            String barbershopName, LocalDateTime startTime, BigDecimal totalPrice) {

        // IN_APP — cliente
        createNotification(customerId, NotificationType.APPOINTMENT_CREATED,
                "Agendamento confirmado!",
                String.format("Seu agendamento na %s com %s está confirmado para %s. Valor: R$ %.2f",
                        barbershopName, barberName,
                        startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")),
                        totalPrice));
        pushNotificationService.sendToUser(customerId,
                "Agendamento confirmado!",
                String.format("%s com %s em %s", barbershopName, barberName,
                        startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))),
                pushData(NotificationType.APPOINTMENT_CREATED, "/meus-agendamentos"));

        // IN_APP — barbeiro
        createNotification(barberId, NotificationType.APPOINTMENT_CREATED,
                "Novo agendamento!",
                String.format("Novo agendamento com %s em %s. Valor: R$ %.2f",
                        customerName,
                        startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")),
                        totalPrice));
        pushNotificationService.sendToUser(barberId,
                "Novo agendamento!",
                String.format("Cliente %s em %s", customerName,
                        startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))),
                pushData(NotificationType.APPOINTMENT_CREATED, "/barberHome"));

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            try {
                emailService.sendAppointmentConfirmedToCustomer(
                        customerEmail, customerName, barbershopName, barberName, startTime, totalPrice);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail appointment.created ao cliente {}: {}", customerEmail, e.getMessage());
            }
        }

        // E-mail — barbeiro
        if (barberEmail != null && !barberEmail.isBlank()) {
            try {
                emailService.sendNewAppointmentToBarber(
                        barberEmail, barberName, customerName, startTime, totalPrice);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail appointment.created ao barbeiro {}: {}", barberEmail, e.getMessage());
            }
        }
    }

    // ─── Agendamento cancelado ───────────────────────────────────────────────────

    @Transactional
    public void notifyAppointmentCancelled(
            UUID customerId, String customerEmail, String customerName,
            UUID barberId, String barberEmail, String barberName,
            String barbershopName, LocalDateTime startTime, String cancelledBy) {

        if ("CUSTOMER".equals(cancelledBy)) {
            // IN_APP — barbeiro
            createNotification(barberId, NotificationType.APPOINTMENT_CANCELLED,
                    "Agendamento cancelado",
                    String.format("O cliente %s cancelou o agendamento de %s.",
                            customerName,
                            startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))));
            pushNotificationService.sendToUser(barberId,
                    "Agendamento cancelado",
                    String.format("%s cancelou o horário de %s", customerName,
                            startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))),
                    pushData(NotificationType.APPOINTMENT_CANCELLED, "/barberHome"));

            // E-mail — barbeiro
            if (barberEmail != null && !barberEmail.isBlank()) {
                try {
                    emailService.sendCancelledByCustomerToBarber(
                            barberEmail, barberName, customerName, startTime);
                } catch (Exception e) {
                    log.warn("Falha ao enviar e-mail cancelled-by-customer ao barbeiro {}: {}", barberEmail, e.getMessage());
                }
            }
        } else {
            // IN_APP — cliente
            createNotification(customerId, NotificationType.APPOINTMENT_CANCELLED,
                    "Agendamento cancelado",
                    "O barbeiro cancelou o seu agendamento. Tente agendar novamente.");
            pushNotificationService.sendToUser(customerId,
                    "Agendamento cancelado",
                    "O barbeiro cancelou seu horário.",
                    pushData(NotificationType.APPOINTMENT_CANCELLED, "/meus-agendamentos"));

            // E-mail — cliente
            if (customerEmail != null && !customerEmail.isBlank()) {
                try {
                    emailService.sendCancelledByBarberToCustomer(
                            customerEmail, customerName, barbershopName, barberName, startTime);
                } catch (Exception e) {
                    log.warn("Falha ao enviar e-mail cancelled-by-barber ao cliente {}: {}", customerEmail, e.getMessage());
                }
            }
        }
    }

    // ─── Atendimento concluído ───────────────────────────────────────────────────

    @Transactional
    public void notifyAppointmentConcluded(
            UUID customerId, String customerEmail, String customerName,
            String barberName, String barbershopName) {

        // IN_APP — cliente
        createNotification(customerId, NotificationType.APPOINTMENT_CONCLUDED,
                "Atendimento concluído!",
                "Seu atendimento foi concluído. Que tal deixar uma avaliação?");
        pushNotificationService.sendToUser(customerId,
                "Atendimento concluído!",
                "Seu atendimento foi finalizado. Deixe uma avaliação.",
                pushData(NotificationType.APPOINTMENT_CONCLUDED, "/meus-agendamentos"));

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            try {
                emailService.sendConcludedToCustomer(
                        customerEmail, customerName, barberName, barbershopName);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail concluded ao cliente {}: {}", customerEmail, e.getMessage());
            }
        }
    }

    // ─── Atendimento reagendado ────────────────────────────────────────────────

    @Transactional
    public void notifyAppointmentRescheduled(
            UUID customerId, String customerEmail, String customerName,
            UUID barberId, String barberEmail, String barberName,
            String barbershopName, LocalDateTime oldStartTime, LocalDateTime newStartTime) {

        String oldSlot = oldStartTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        String newSlot = newStartTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));

        // IN_APP — cliente
        createNotification(customerId, NotificationType.APPOINTMENT_RESCHEDULED,
                "Agendamento reagendado",
                String.format("Seu horario em %s foi alterado de %s para %s.",
                        barbershopName, oldSlot, newSlot));
        pushNotificationService.sendToUser(customerId,
                "Agendamento reagendado",
                String.format("Novo horário: %s", newSlot),
                pushData(NotificationType.APPOINTMENT_RESCHEDULED, "/meus-agendamentos"));

        // IN_APP — barbeiro
        createNotification(barberId, NotificationType.APPOINTMENT_RESCHEDULED,
                "Agendamento reagendado",
                String.format("Atendimento com %s foi alterado de %s para %s.",
                        customerName, oldSlot, newSlot));
        pushNotificationService.sendToUser(barberId,
                "Agendamento reagendado",
                String.format("Atendimento de %s agora em %s", customerName, newSlot),
                pushData(NotificationType.APPOINTMENT_RESCHEDULED, "/barberHome"));

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            try {
                emailService.sendRescheduledToCustomer(
                        customerEmail, customerName, barbershopName, barberName, oldStartTime, newStartTime);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail rescheduled ao cliente {}: {}", customerEmail, e.getMessage());
            }
        }

        // E-mail — barbeiro
        if (barberEmail != null && !barberEmail.isBlank()) {
            try {
                emailService.sendRescheduledToBarber(
                        barberEmail, barberName, customerName, oldStartTime, newStartTime);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail rescheduled ao barbeiro {}: {}", barberEmail, e.getMessage());
            }
        }
    }

    // ─── Pagamento aprovado ──────────────────────────────────────────────────────

    @Transactional
    public void notifyPaymentApproved(
            UUID customerId, String customerEmail, BigDecimal amount, UUID appointmentId) {

        // IN_APP — cliente
        createNotification(customerId, NotificationType.PAYMENT_APPROVED,
                "Pagamento aprovado!",
                String.format("Seu pagamento de R$ %.2f foi aprovado com sucesso.", amount));
        pushNotificationService.sendToUser(customerId,
                "Pagamento aprovado!",
                String.format("Pagamento de R$ %.2f confirmado.", amount),
                pushData(NotificationType.PAYMENT_APPROVED, "/meus-agendamentos"));

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            try {
                emailService.sendPaymentApprovedToCustomer(customerEmail, "Cliente", amount);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail payment.approved ao cliente {}: {}", customerEmail, e.getMessage());
            }
        }

        // IN_APP + Push — barbeiro (via feign ao schedule-service)
        if (appointmentId != null) {
            try {
                AppointmentInfoDTO appt = scheduleServiceClient.getAppointmentById(appointmentId);
                if (appt != null && appt.getBarberId() != null) {
                    createNotification(appt.getBarberId(), NotificationType.PAYMENT_APPROVED,
                            "Pagamento recebido!",
                            String.format("O pagamento de R$ %.2f referente ao agendamento foi confirmado.", amount));
                    pushNotificationService.sendToUser(appt.getBarberId(),
                            "Pagamento recebido!",
                            String.format("R$ %.2f confirmado para %s", amount, appt.getBarbershopName()),
                            pushData(NotificationType.PAYMENT_APPROVED, "/barberHome"));
                }
            } catch (Exception e) {
                log.warn("Não foi possível notificar barbeiro do pagamento: appointmentId={} — {}", appointmentId, e.getMessage());
            }
        }
    }

    // ─── Lembrete de agendamento ─────────────────────────────────────────────────

    @Transactional
    public void notifyAppointmentReminder(AppointmentReminderEvent event) {
        // IN_APP + Push — cliente
        createNotification(event.getCustomerId(), NotificationType.APPOINTMENT_REMINDER,
                "Seu horário está chegando!",
                String.format("Lembrete: você tem um agendamento em %s com %s às %s.",
                        event.getBarbershopName(), event.getBarberName(),
                        event.getStartTime().toLocalTime().toString()));
        pushNotificationService.sendToUser(event.getCustomerId(),
                "Seu horário está chegando!",
                String.format("%s — %s", event.getBarbershopName(),
                        event.getStartTime().toLocalTime().toString()),
                pushData(NotificationType.APPOINTMENT_REMINDER, "/meus-agendamentos"));
        if (event.getCustomerEmail() != null && !event.getCustomerEmail().isBlank()) {
            try {
                emailService.sendReminderToCustomer(event.getCustomerEmail(), event.getCustomerName(),
                        event.getBarbershopName(), event.getBarberName(), event.getStartTime());
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail reminder ao cliente {}: {}", event.getCustomerEmail(), e.getMessage());
            }
        }

        // IN_APP + Push — barbeiro
        if (event.getBarberId() != null) {
            createNotification(event.getBarberId(), NotificationType.APPOINTMENT_REMINDER,
                    "Lembrete de atendimento",
                    String.format("Você tem um atendimento com %s às %s na %s.",
                            event.getCustomerName(),
                            event.getStartTime().toLocalTime().toString(),
                            event.getBarbershopName()));
            pushNotificationService.sendToUser(event.getBarberId(),
                    "Lembrete de atendimento",
                    String.format("%s às %s", event.getCustomerName(),
                            event.getStartTime().toLocalTime().toString()),
                    pushData(NotificationType.APPOINTMENT_REMINDER, "/barberHome"));
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /**
     * Cria uma notificação IN_APP para o usuário.
     */
    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Notificação criada [{}] para userId={}: {}", type, userId, title);

        // Captura count e DTO ainda dentro da transação (leitura consistente)
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);
        NotificationDTO dto = toDTO(saved);

        // Dispara SSE APÓS o commit para evitar race condition:
        // sem isso, o cliente recebe o evento e chama fetchNotifications antes do
        // banco confirmar a gravação, retornando lista vazia.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sseEmitterService.sendUnreadCount(userId, unreadCount);
                    sseEmitterService.sendNotificationCreated(userId, dto, unreadCount);
                }
            });
        } else {
            sseEmitterService.sendUnreadCount(userId, unreadCount);
            sseEmitterService.sendNotificationCreated(userId, dto, unreadCount);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDTO markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Notificação não encontrada: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Notificação não pertence ao usuário solicitante");
        }
        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        long unreadCount = notificationRepository.countByUserIdAndReadFalse(userId);

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sseEmitterService.sendUnreadCount(userId, unreadCount);
                }
            });
        } else {
            sseEmitterService.sendUnreadCount(userId, unreadCount);
        }

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    // ─── Pedido de entrada em barbearia ─────────────────────────────────────────

    @Transactional
    public void notifyJoinRequestReceived(
            UUID ownerId, String ownerEmail, String barbershopName, String barberName) {

        // IN_APP — dono da barbearia
        createNotification(ownerId, NotificationType.JOIN_REQUEST_RECEIVED,
                "Novo pedido de entrada!",
                String.format("O barbeiro %s quer entrar na sua barbearia %s. Acesse 'Meu Time' para aprovar ou recusar.",
                        barberName, barbershopName));
        pushNotificationService.sendToUser(ownerId,
                "Novo pedido de entrada!",
                String.format("%s quer entrar na barbearia %s", barberName, barbershopName),
                pushData(NotificationType.JOIN_REQUEST_RECEIVED, "/barberHome/time"));

        // E-mail — dono
        if (ownerEmail != null && !ownerEmail.isBlank()) {
            try {
                emailService.sendJoinRequestReceivedToOwner(ownerEmail, barbershopName, barberName);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail join-request ao owner {}: {}", ownerEmail, e.getMessage());
            }
        }

        log.info("event=join-request-notification-created ownerId={} barberName={} shop={}",
                ownerId, barberName, barbershopName);
    }

    @Transactional
    public void notifyInviteReceived(UUID barberId, String barberEmail, String barbershopName) {
        // IN_APP — barbeiro convidado
        createNotification(barberId, NotificationType.INVITE_RECEIVED,
                "Você recebeu um convite!",
                String.format("A barbearia %s convidou você para fazer parte da equipe. Acesse 'Meu Perfil' para aceitar ou recusar.",
                        barbershopName));
        pushNotificationService.sendToUser(barberId,
                "Você recebeu um convite!",
                String.format("A barbearia %s convidou você para o time.", barbershopName),
                pushData(NotificationType.INVITE_RECEIVED, "/barberHome/perfil"));

        // E-mail — barbeiro convidado
        if (barberEmail != null && !barberEmail.isBlank()) {
            try {
                emailService.sendInviteReceivedToBarber(barberEmail, barbershopName);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail invite ao barbeiro {}: {}", barberEmail, e.getMessage());
            }
        }

        log.info("event=invite-notification-created barberId={} shop={}", barberId, barbershopName);
    }

    // ─── Barbeiro removido da barbearia ─────────────────────────────────────────

    @Transactional
    public void notifyBarberRemoved(UUID barberId, String barberEmail, String barberName, String barbershopName) {

        createNotification(barberId, NotificationType.BARBER_REMOVED,
                "Você foi removido da barbearia",
                String.format("O dono da barbearia %s removeu você da equipe.", barbershopName));
        pushNotificationService.sendToUser(barberId,
                "Você foi removido da barbearia",
                String.format("Você não faz mais parte da equipe %s.", barbershopName),
                pushData(NotificationType.BARBER_REMOVED, "/barberHome/perfil"));

        if (barberEmail != null && !barberEmail.isBlank()) {
            try {
                emailService.sendBarberRemovedToBarber(barberEmail, barberName, barbershopName);
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail barber-removed ao barbeiro {}: {}", barberEmail, e.getMessage());
            }
        }

        log.info("event=barber-removed-notification-created barberId={} shop={}", barberId, barbershopName);
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(), n.getUserId(), n.getType(), n.getTitle(),
                n.getMessage(), n.getChannel(), n.isRead(), n.getCreatedAt());
    }

        private Map<String, String> pushData(NotificationType type, String deepLink) {
                Map<String, String> data = new HashMap<>();
                data.put("type", type.name());
                data.put("deepLink", deepLink);
                return data;
        }
}
