package ifsp.edu.projeto.cortaai.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessaging;
import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;
    @Mock
    private DeviceTokenService deviceTokenService;
    @Mock
    private ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    private PushNotificationService pushNotificationService;

    @BeforeEach
    void setUp() {
        pushNotificationService = new PushNotificationService(
                deviceTokenRepository,
                deviceTokenService,
                firebaseMessagingProvider);
    }

    @Test
    void shouldSkipBeforeQueryingTokensWhenPushIsDisabled() {
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", false);

        pushNotificationService.sendToUser(userId, "Titulo", "Mensagem", Map.of("type", "TEST"));

        verifyNoInteractions(deviceTokenRepository, deviceTokenService, firebaseMessagingProvider);
    }

    @Test
    void shouldSkipFirebaseWhenUserHasNoActiveTokens() {
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        pushNotificationService.sendToUser(userId, "Titulo", "Mensagem", Map.of("type", "TEST"));

        verify(deviceTokenRepository).findByUserIdAndActiveTrue(userId);
        verifyNoInteractions(deviceTokenService, firebaseMessagingProvider);
    }
}
