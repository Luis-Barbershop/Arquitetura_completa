package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.feign.UserInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.service.NotificationService;
import ifsp.edu.projeto.cortaai.notificationservice.service.SseEmitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSseControllerTest {

    @Mock
    private SseEmitterService sseEmitterService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserServiceClient userServiceClient;

    private NotificationSseController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationSseController(sseEmitterService, notificationService, userServiceClient);
    }

    @Test
    void shouldOpenSseStreamAndRegisterEmitter() {
        UUID userId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(userId));
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        SseEmitter emitter = controller.stream("firebase-uid");

        assertThat(emitter).isNotNull();
        verify(sseEmitterService).register(eq(userId), any(SseEmitter.class));
        verify(notificationService).getUnreadCount(userId);
    }

    @Test
    void shouldFailWhenFirebaseUidCannotBeResolved() {
        when(userServiceClient.getUserByFirebaseUid("missing-uid")).thenReturn(null);

        assertThatThrownBy(() -> controller.stream("missing-uid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(sseEmitterService, never()).register(any(), any());
    }

    private static UserInfoDTO user(UUID userId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(userId);
        user.setFirebaseUid("firebase-uid");
        return user;
    }
}
