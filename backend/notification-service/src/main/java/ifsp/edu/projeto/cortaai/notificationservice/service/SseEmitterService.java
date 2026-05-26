package ifsp.edu.projeto.cortaai.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ifsp.edu.projeto.cortaai.notificationservice.dto.NotificationDTO;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia conexões SSE ativas por userId interno (UUID do banco).
 * Thread-safe via ConcurrentHashMap.
 */
@Service
@Slf4j
public class SseEmitterService {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(UUID userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        log.debug("SSE registrado para userId={} — conexões ativas={}", userId, emitters.size());
    }

    public void remove(UUID userId) {
        emitters.remove(userId);
        log.debug("SSE removido para userId={} — conexões ativas={}", userId, emitters.size());
    }

    /**
     * Envia o evento {@code unread-count} ao usuário se ele tiver conexão SSE ativa.
     * Em caso de erro de escrita, remove o emitter da registry.
     */
    public void sendUnreadCount(UUID userId, long count) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("unread-count")
                    .data(Map.of("unreadCount", count)));
            log.debug("SSE unread-count={} enviado para userId={}", count, userId);
        } catch (IOException e) {
            log.warn("Falha ao enviar SSE para userId={} — removendo emitter: {}", userId, e.getMessage());
            emitters.remove(userId);
        }
    }

    public void sendNotificationCreated(UUID userId, NotificationDTO notification, long unreadCount) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event()
                    .name("notification-created")
                    .data(Map.of(
                            "notification", notification,
                            "unreadCount", unreadCount
                    )));
            log.debug("SSE notification-created enviado para userId={}", userId);
        } catch (IOException e) {
            log.warn("Falha ao enviar SSE para userId={} — removendo emitter: {}", userId, e.getMessage());
            emitters.remove(userId);
        }
    }
}
