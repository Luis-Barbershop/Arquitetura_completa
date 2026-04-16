package ifsp.edu.projeto.cortaai.notificationservice.service;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.model.Notification;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationChannel;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;
import ifsp.edu.projeto.cortaai.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

        // IN_APP — barbeiro
        createNotification(barberId, NotificationType.APPOINTMENT_CREATED,
                "Novo agendamento!",
                String.format("Novo agendamento com %s em %s. Valor: R$ %.2f",
                        customerName,
                        startTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")),
                        totalPrice));

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            emailService.sendAppointmentConfirmedToCustomer(
                    customerEmail, customerName, barbershopName, barberName, startTime, totalPrice);
        }

        // E-mail — barbeiro
        if (barberEmail != null && !barberEmail.isBlank()) {
            emailService.sendNewAppointmentToBarber(
                    barberEmail, barberName, customerName, startTime, totalPrice);
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

            // E-mail — barbeiro
            if (barberEmail != null && !barberEmail.isBlank()) {
                emailService.sendCancelledByCustomerToBarber(
                        barberEmail, barberName, customerName, startTime);
            }
        } else {
            // IN_APP — cliente
            createNotification(customerId, NotificationType.APPOINTMENT_CANCELLED,
                    "Agendamento cancelado",
                    "O barbeiro cancelou o seu agendamento. Tente agendar novamente.");

            // E-mail — cliente
            if (customerEmail != null && !customerEmail.isBlank()) {
                emailService.sendCancelledByBarberToCustomer(
                        customerEmail, customerName, barbershopName, barberName, startTime);
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

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            emailService.sendConcludedToCustomer(
                    customerEmail, customerName, barberName, barbershopName);
        }
    }

    // ─── Pagamento aprovado ──────────────────────────────────────────────────────

    @Transactional
    public void notifyPaymentApproved(
            UUID customerId, String customerEmail, BigDecimal amount) {

        // IN_APP — cliente
        createNotification(customerId, NotificationType.PAYMENT_APPROVED,
                "Pagamento aprovado!",
                String.format("Seu pagamento de R$ %.2f foi aprovado com sucesso.", amount));

        // E-mail — cliente
        if (customerEmail != null && !customerEmail.isBlank()) {
            emailService.sendPaymentApprovedToCustomer(customerEmail, "Cliente", amount);
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
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada: " + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Notificação não pertence ao usuário");
        }
        notification.setRead(true);
        return toDTO(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    // ─── Pedido de entrada em barbearia ─────────────────────────────────────────

    @Transactional
    public void notifyJoinRequestReceived(
            UUID ownerId, String barbershopName, String barberName) {

        // IN_APP — dono da barbearia
        createNotification(ownerId, NotificationType.JOIN_REQUEST_RECEIVED,
                "Novo pedido de entrada!",
                String.format("O barbeiro %s quer entrar na sua barbearia %s. Acesse 'Meu Time' para aprovar ou recusar.",
                        barberName, barbershopName));

        log.info("event=join-request-notification-created ownerId={} barberName={} shop={}",
                ownerId, barberName, barbershopName);
    }

    @Transactional
    public void notifyInviteReceived(UUID barberId, String barbershopName) {

        // IN_APP — barbeiro convidado
        createNotification(barberId, NotificationType.INVITE_RECEIVED,
                "Você recebeu um convite!",
                String.format("A barbearia %s convidou você para fazer parte da equipe. Acesse 'Meu Perfil' para aceitar ou recusar.",
                        barbershopName));

        log.info("event=invite-notification-created barberId={} shop={}", barberId, barbershopName);
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(), n.getUserId(), n.getType(), n.getTitle(),
                n.getMessage(), n.getChannel(), n.isRead(), n.getCreatedAt());
    }
}
