package ifsp.edu.projeto.cortaai.scheduleservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gerencia o histórico de conversa do Gustavo por usuário no Redis.
 * Chave: gustavo:history:{firebaseUid}
 * TTL: 2 horas de inatividade — sessão expira automaticamente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private static final String KEY_PREFIX  = "gustavo:history:";
    private static final int    MAX_TURNS   = 10;   // últimos 10 pares (user + assistant)
    private static final Duration SESSION_TTL = Duration.ofHours(2);

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** Retorna os últimos {@code MAX_TURNS} turnos da conversa do usuário. */
    public List<Map<String, String>> getHistory(String firebaseUid) {
        String key = KEY_PREFIX + firebaseUid;
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("gustavo: falha ao ler histórico uid={} — {}", firebaseUid, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Adiciona um turno (user + assistant) ao histórico e persiste no Redis.
     * Mantém apenas os últimos {@code MAX_TURNS} turnos.
     */
    public void appendTurn(String firebaseUid, String userMessage, String assistantReply) {
        String key = KEY_PREFIX + firebaseUid;
        try {
            List<Map<String, String>> history = getHistory(firebaseUid);
            history.add(Map.of("role", "user",      "content", userMessage));
            history.add(Map.of("role", "assistant", "content", assistantReply));

            // Mantém apenas os últimos MAX_TURNS * 2 elementos (cada turno = 2 mensagens)
            int maxEntries = MAX_TURNS * 2;
            if (history.size() > maxEntries) {
                history = history.subList(history.size() - maxEntries, history.size());
            }

            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(history), SESSION_TTL);
        } catch (Exception e) {
            log.warn("gustavo: falha ao salvar histórico uid={} — {}", firebaseUid, e.getMessage());
        }
    }

    /** Apaga o histórico do usuário (ex: botão "Nova conversa"). */
    public void clearHistory(String firebaseUid) {
        try {
            stringRedisTemplate.delete(KEY_PREFIX + firebaseUid);
        } catch (Exception e) {
            log.warn("gustavo: falha ao limpar histórico uid={} — {}", firebaseUid, e.getMessage());
        }
    }

    /** Formata o histórico como bloco de texto para o prompt. */
    public String formatHistoryForPrompt(List<Map<String, String>> history) {
        if (history.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("HISTÓRICO DA CONVERSA ATUAL:\n");
        for (Map<String, String> msg : history) {
            String role = "user".equals(msg.get("role")) ? "Usuário" : "Gustavo";
            sb.append(role).append(": ").append(msg.get("content")).append('\n');
        }
        return sb.toString();
    }
}
