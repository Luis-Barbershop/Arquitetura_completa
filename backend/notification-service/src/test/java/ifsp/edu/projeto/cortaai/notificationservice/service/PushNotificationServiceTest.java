package ifsp.edu.projeto.cortaai.notificationservice.service;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import ifsp.edu.projeto.cortaai.notificationservice.model.DeviceToken;
import ifsp.edu.projeto.cortaai.notificationservice.model.PushPlatform;
import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void shouldSkipWhenFirebaseMessagingIsUnavailable() {
        UUID userId = UUID.randomUUID();
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(deviceToken("long-token-123456")));
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(null);

        pushNotificationService.sendToUser(userId, "Titulo", "Mensagem", Map.of("type", "TEST"));

        verify(firebaseMessagingProvider).getIfAvailable();
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void shouldSendNotificationToAllActiveTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(
                deviceToken("token-one-123456"),
                deviceToken("short")
        ));
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(firebaseMessaging);
        when(firebaseMessaging.send(any(Message.class))).thenReturn("message-id");

        pushNotificationService.sendToUser(userId, "Titulo", "Mensagem", Map.of("type", "TEST"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging, times(2)).send(messageCaptor.capture());
        verifyNoInteractions(deviceTokenService);
        assertThat(messageCaptor.getAllValues()).hasSize(2);
    }

    @Test
    void shouldTolerateGenericFirebaseSendFailures() throws Exception {
        UUID userId = UUID.randomUUID();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(deviceToken("token-one-123456")));
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(firebaseMessaging);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("network down"));

        pushNotificationService.sendToUser(userId, "Titulo", "Mensagem", null);

        verify(firebaseMessaging).send(any(Message.class));
        verifyNoInteractions(deviceTokenService);
    }

    @Test
    void shouldDeactivateInvalidFirebaseTokens() throws Exception {
        UUID userId = UUID.randomUUID();
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        ReflectionTestUtils.setField(pushNotificationService, "pushEnabled", true);
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(deviceToken("invalid-token-123456")));
        when(firebaseMessagingProvider.getIfAvailable()).thenReturn(firebaseMessaging);
        when(firebaseMessaging.send(any(Message.class))).thenThrow(firebaseMessagingException(MessagingErrorCode.UNREGISTERED));

        pushNotificationService.sendToUser(userId, "Titulo", "Mensagem", Map.of());

        verify(deviceTokenService).deactivateTokenByValue("invalid-token-123456");
    }

    private static DeviceToken deviceToken(String token) {
        return DeviceToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .platform(PushPlatform.WEB)
                .token(token)
                .active(true)
                .build();
    }

    private static FirebaseMessagingException firebaseMessagingException(MessagingErrorCode messagingErrorCode) throws Exception {
        FirebaseException firebaseException = new FirebaseException(ErrorCode.INVALID_ARGUMENT, "invalid token", null);
        Method method = FirebaseMessagingException.class.getDeclaredMethod(
                "withMessagingErrorCode",
                FirebaseException.class,
                MessagingErrorCode.class
        );
        method.setAccessible(true);
        return (FirebaseMessagingException) method.invoke(null, firebaseException, messagingErrorCode);
    }
}
