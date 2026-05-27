package ifsp.edu.projeto.cortaai.notificationservice.controller;

import ifsp.edu.projeto.cortaai.notificationservice.dto.RegisterDeviceTokenRequestDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserInfoDTO;
import ifsp.edu.projeto.cortaai.notificationservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.notificationservice.model.PushPlatform;
import ifsp.edu.projeto.cortaai.notificationservice.service.DeviceTokenService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenControllerTest {

    @Mock
    private DeviceTokenService deviceTokenService;
    @Mock
    private UserServiceClient userServiceClient;

    private DeviceTokenController controller;

    @BeforeEach
    void setUp() {
        controller = new DeviceTokenController(deviceTokenService, userServiceClient);
    }

    @Test
    void shouldRegisterDeviceTokenForResolvedUser() {
        UUID userId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(userId));

        var response = controller.registerToken(
                "firebase-uid",
                new RegisterDeviceTokenRequestDTO("push-token", PushPlatform.WEB));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(deviceTokenService).registerToken(userId, "push-token", PushPlatform.WEB);
    }

    @Test
    void shouldUnregisterDeviceTokenForResolvedUser() {
        UUID userId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(userId));

        var response = controller.unregisterToken("firebase-uid", "push-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(deviceTokenService).deactivateToken(userId, "push-token");
    }

    @Test
    void shouldFailWhenFirebaseUidCannotBeResolved() {
        when(userServiceClient.getUserByFirebaseUid("missing-uid")).thenReturn(new UserInfoDTO());

        assertThatThrownBy(() -> controller.unregisterToken("missing-uid", "push-token"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Usuário não encontrado");
    }

    private static UserInfoDTO user(UUID userId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(userId);
        user.setFirebaseUid("firebase-uid");
        return user;
    }
}
