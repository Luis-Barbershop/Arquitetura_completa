package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST de notificações.
 * Endpoints públicos (expostos via Gateway).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lista todas as notificações do usuário logado.
     * O userId vem do header X-User-Id (injetado pelo Gateway/JWT filter).
     */
    @GetMapping("/my-notifications")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    /**
     * Marca uma notificação como lida.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    /**
     * Retorna a contagem de notificações não lidas.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
