package ifsp.edu.projeto.cortaai.scheduleservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ChatHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ChatHistoryService(redisTemplate, new ObjectMapper());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldReturnEmptyHistoryWhenRedisHasNoDataOrFails() {
        when(valueOperations.get("gustavo:history:uid-1")).thenReturn(" ");

        assertThat(service.getHistory("uid-1")).isEmpty();

        when(valueOperations.get("gustavo:history:uid-2")).thenThrow(new RuntimeException("redis off"));
        assertThat(service.getHistory("uid-2")).isEmpty();
    }

    @Test
    void shouldReadAppendTrimAndPersistHistory() {
        when(valueOperations.get("gustavo:history:uid-1")).thenReturn("""
                [{"role":"user","content":"oi"},{"role":"assistant","content":"ola"}]
                """);

        List<Map<String, String>> history = service.getHistory("uid-1");
        service.appendTurn("uid-1", "agenda", "sem horarios");

        assertThat(history).hasSize(2);
        verify(valueOperations).set(
                eq("gustavo:history:uid-1"),
                contains("sem horarios"),
                eq(Duration.ofHours(2))
        );
    }

    @Test
    void shouldKeepOnlyLastTenConversationTurns() {
        when(valueOperations.get("gustavo:history:uid-1")).thenReturn(null);

        for (int i = 0; i < 12; i++) {
            service.appendTurn("uid-1", "pergunta " + i, "resposta " + i);
        }

        verify(valueOperations, times(12)).set(eq("gustavo:history:uid-1"), anyString(), eq(Duration.ofHours(2)));
    }

    @Test
    void shouldTolerateAppendAndClearFailures() {
        when(valueOperations.get("gustavo:history:uid-1")).thenThrow(new RuntimeException("read off"));
        doThrow(new RuntimeException("write off")).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        doThrow(new RuntimeException("delete off")).when(redisTemplate).delete("gustavo:history:uid-1");

        service.appendTurn("uid-1", "pergunta", "resposta");
        service.clearHistory("uid-1");

        verify(redisTemplate).delete("gustavo:history:uid-1");
    }

    @Test
    void shouldFormatHistoryForPrompt() {
        assertThat(service.formatHistoryForPrompt(List.of())).isEmpty();

        String prompt = service.formatHistoryForPrompt(List.of(
                Map.of("role", "user", "content", "oi"),
                Map.of("role", "assistant", "content", "ola")
        ));

        assertThat(prompt).contains("HISTÓRICO", "Usuário: oi", "Gustavo: ola");
    }
}
