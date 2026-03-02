package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notifications", description = "NOVO: Endpoints para consulta e gestão de notificações dos usuários (geradas via mensageria)")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lista todas as notificações do usuário logado.
     * O userId vem do header X-User-Id (injetado pelo Gateway/JWT filter).
     */
    @Operation(summary = "Listar minhas notificações", description = "Retorna todas as notificações (lidas e não lidas) do usuário logado.")
    @GetMapping("/my-notifications")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(
            @Parameter(description = "ID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    /**
     * Marca uma notificação como lida.
     */
    @Operation(summary = "Marcar notificação como lida", description = "Atualiza o status de uma notificação específica para 'lida'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificação atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada ou não pertence ao usuário")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @Parameter(description = "UUID da notificação") @PathVariable UUID id,
            @Parameter(description = "ID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    /**
     * Retorna a contagem de notificações não lidas.
     */
    @Operation(summary = "Contagem de não lidas", description = "Retorna o número total de notificações que o usuário ainda não leu. Útil para badges no frontend.")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(description = "ID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}