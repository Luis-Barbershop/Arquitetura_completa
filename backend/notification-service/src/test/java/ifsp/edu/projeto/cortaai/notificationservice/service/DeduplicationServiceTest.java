package ifsp.edu.projeto.cortaai.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private DeduplicationService deduplicationService;

    @Test
    void shouldMarkFirstEventAsNotDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofHours(24))))
                .thenReturn(true);

        boolean duplicate = deduplicationService.isDuplicate("PAYMENT", "abc");

        assertThat(duplicate).isFalse();
        verify(valueOperations).setIfAbsent(
                eq("notification:PAYMENT:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
                eq("1"),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void shouldDetectDuplicateWhenKeyAlreadyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofHours(24))))
                .thenReturn(false);

        boolean duplicate = deduplicationService.isDuplicate("PAYMENT", "abc");

        assertThat(duplicate).isTrue();
        verify(valueOperations).setIfAbsent(
                eq("notification:PAYMENT:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
                eq("1"),
                eq(Duration.ofHours(24))
        );
    }
}
