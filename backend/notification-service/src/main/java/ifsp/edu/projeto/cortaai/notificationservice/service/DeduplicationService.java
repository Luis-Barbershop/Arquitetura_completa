package ifsp.edu.projeto.cortaai.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Serviço de deduplicação via Redis.
 * Evita notificações duplicadas usando chave com TTL de 24h.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration TTL = Duration.ofHours(24);

    /**
     * Verifica se a notificação já foi processada.
     * Se não, marca como processada e retorna false.
     * Se sim, retorna true (duplicada).
     *
     * @param type tipo do evento
     * @param id   id único do evento
     * @return true se é duplicata
     */
    public boolean isDuplicate(String type, String id) {
        String key = "notification:" + type + ":" + id;
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        if (Boolean.FALSE.equals(wasSet)) {
            log.warn("Notificação duplicada detectada: {}", key);
            return true;
        }
        return false;
    }
}
