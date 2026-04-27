package ifsp.edu.projeto.cortaai.notificationservice.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import ifsp.edu.projeto.cortaai.notificationservice.model.DeviceToken;
import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenService deviceTokenService;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    @Value("${notification.push.enabled:false}")
    private boolean pushEnabled;

    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        if (!pushEnabled) {
            return;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.warn("event=push-skipped reason=firebase-messaging-unavailable userId={}", userId);
            return;
        }

        for (DeviceToken token : deviceTokenRepository.findByUserIdAndActiveTrue(userId)) {
            try {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                        .putAllData(data)
                        .build();

                firebaseMessaging.send(message);
            } catch (FirebaseMessagingException ex) {
                log.warn("event=push-send-failed userId={} errorCode={} message={}",
                        userId,
                        ex.getMessagingErrorCode(),
                        ex.getMessage());

                if (shouldDeactivateToken(ex)) {
                    deviceTokenService.deactivateTokenByValue(token.getToken());
                }
            } catch (Exception ex) {
                log.warn("event=push-send-generic-failed userId={} error={}", userId, ex.getMessage());
            }
        }
    }

    private boolean shouldDeactivateToken(FirebaseMessagingException ex) {
        return ex.getMessagingErrorCode() != null && (
                ex.getMessagingErrorCode().name().equals("UNREGISTERED")
                        || ex.getMessagingErrorCode().name().equals("INVALID_ARGUMENT")
        );
    }
}
