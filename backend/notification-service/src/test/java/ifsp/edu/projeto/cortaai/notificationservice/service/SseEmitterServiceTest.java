package ifsp.edu.projeto.cortaai.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

class SseEmitterServiceTest {

    private final SseEmitterService service = new SseEmitterService();

    @Test
    void shouldSendUnreadCountToRegisteredEmitter() throws Exception {
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        service.register(userId, emitter);

        service.sendUnreadCount(userId, 7L);

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldIgnoreSendWhenUserHasNoEmitter() throws Exception {
        SseEmitter emitter = mock(SseEmitter.class);

        service.sendUnreadCount(UUID.randomUUID(), 3L);

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldRemoveEmitterAfterSendFailure() throws Exception {
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("connection closed"))
                .when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        service.register(userId, emitter);

        service.sendUnreadCount(userId, 1L);
        service.sendUnreadCount(userId, 2L);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void shouldStopSendingAfterExplicitRemove() throws Exception {
        UUID userId = UUID.randomUUID();
        SseEmitter emitter = mock(SseEmitter.class);
        service.register(userId, emitter);

        service.remove(userId);
        service.sendUnreadCount(userId, 4L);

        verify(emitter, never()).send(any(SseEmitter.SseEventBuilder.class));
    }
}
