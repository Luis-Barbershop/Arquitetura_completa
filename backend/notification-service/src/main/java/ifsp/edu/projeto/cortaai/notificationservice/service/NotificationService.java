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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Serviço de notificações — orquestra a criação e envio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

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

    /**
     * Busca todas as notificações do usuário, ordenadas por data.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Marca uma notificação como lida.
     */
    @Transactional
    public NotificationDTO markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada: " + notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Notificação não pertence ao usuário");
        }

        notification.setRead(true);
        Notification saved = notificationRepository.save(notification);
        return toDTO(saved);
    }

    /**
     * Conta notificações não lidas do usuário.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getChannel(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
