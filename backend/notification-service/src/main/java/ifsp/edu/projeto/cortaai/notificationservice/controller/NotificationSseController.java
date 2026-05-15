package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.feign.UserInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import ifsp.edu.projeto.cortaai.notificationservice.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint SSE para notificações em tempo real.
 *
 * <p>O browser abre {@code GET /api/notifications/stream} e mantém a conexão aberta.
 * Sempre que uma nova notificação for salva para o usuário, o servidor empurra
 * um evento {@code unread-count} com a contagem atualizada — eliminando o polling de 30s.
 *
 * <p>Autenticação: o header {@code X-User-UID} (Firebase UID) é injetado pelo api-gateway.
 * O token chega como {@code ?token=} query param (EventSource nativo não suporta headers
 * customizados) e o gateway o converte para {@code Authorization: Bearer} antes de rotear.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationSseController {

    private final SseEmitterService sseEmitterService;
    private final NotificationService notificationService;
    private final UserServiceClient userServiceClient;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestHeader("X-User-UID") String firebaseUid) {

        UUID userId = resolveUserId(firebaseUid);

        // Timeout 0 = sem timeout gerenciado pelo Spring (conexão mantida até o cliente fechar)
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> sseEmitterService.remove(userId));
        emitter.onTimeout(() -> sseEmitterService.remove(userId));
        emitter.onError(ex -> {
            log.debug("SSE erro para userId={}: {}", userId, ex.getMessage());
            sseEmitterService.remove(userId);
        });

        sseEmitterService.register(userId, emitter);

        // Envia a contagem atual imediatamente ao conectar
        long currentCount = notificationService.getUnreadCount(userId);
        try {
            emitter.send(SseEmitter.event()
                    .name("unread-count")
                    .data(Map.of("unreadCount", currentCount)));
        } catch (IOException e) {
            log.warn("Falha ao enviar contagem inicial SSE para userId={}", userId);
            sseEmitterService.remove(userId);
        }

        log.info("SSE stream aberto para userId={} unreadCount={}", userId, currentCount);
        return emitter;
    }

    private UUID resolveUserId(String firebaseUid) {
        UserInfoDTO user = userServiceClient.getUserByFirebaseUid(firebaseUid);
        if (user == null || user.getId() == null) {
            throw new RuntimeException("Usuário não encontrado para o UID: " + firebaseUid);
        }
        return user.getId();
    }
}
