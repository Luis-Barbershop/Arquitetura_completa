package ifsp.edu.projeto.cortaai.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

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
        String key = "notification:" + safeKeyPart(type) + ":" + sha256Hex(id);
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL);
        if (Boolean.FALSE.equals(wasSet)) {
            log.warn("event=notification-duplicate-detected type={} keyHash={}", safeKeyPart(type), sha256Hex(id));
            return true;
        }
        return false;
    }

    private String safeKeyPart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível para deduplicação.", ex);
        }
    }
}
