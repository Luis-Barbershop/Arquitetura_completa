package ifsp.edu.projeto.cortaai.notificationservice.dto;

import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationChannel;
import ifsp.edu.projeto.cortaai.notificationservice.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para retorno de notificações na API.
 */
public record NotificationDTO(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        NotificationChannel channel,
        boolean read,
        LocalDateTime createdAt
) {}
