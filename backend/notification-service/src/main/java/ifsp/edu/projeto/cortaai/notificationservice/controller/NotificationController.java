package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;
import ifsp.edu.projeto.cortaai.notificationservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Recebe X-User-UID (Firebase UID injetado pelo Gateway) e resolve para o UUID do banco.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Endpoints para consulta e gestão de notificações dos usuários")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserServiceClient userServiceClient;

    /**
     * Resolve o Firebase UID para o UUID interno do banco.
     */
    private UUID resolveUserId(String firebaseUid) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Usuário não encontrado para o UID: " + firebaseUid);
        }
        return user.getId();
    }

    @Operation(summary = "Listar minhas notificações",
               description = "Retorna todas as notificações (lidas e não lidas) do usuário logado.")
    @GetMapping("/my-notifications")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        UUID userId = resolveUserId(firebaseUid);
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @Operation(summary = "Marcar notificação como lida",
               description = "Atualiza o status de uma notificação específica para 'lida'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notificação atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(
            @Parameter(description = "UUID da notificação") @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        UUID userId = resolveUserId(firebaseUid);
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }

    @Operation(summary = "Contagem de não lidas",
               description = "Retorna o número total de notificações não lidas. Útil para badges no frontend.")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        UUID userId = resolveUserId(firebaseUid);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
