package ifsp.edu.projeto.cortaai.notificationservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Configuration
@Slf4j
public class FirebaseAdminConfig {

    @Value("${notification.push.service-account-base64:}")
    private String serviceAccountBase64;

    @Value("${notification.push.service-account-path:}")
    private String serviceAccountPath;

    @Value("${notification.push.project-id:}")
    private String projectId;

    @Bean
    @ConditionalOnProperty(prefix = "notification.push", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        Optional<FirebaseApp> existing = FirebaseApp.getApps().stream().findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        try (InputStream credentialsStream = resolveCredentialsStream()) {
            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream));

            if (!projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId);
            }

            FirebaseApp app = FirebaseApp.initializeApp(optionsBuilder.build(), "cortaai-notification-service");
            log.info("event=firebase-admin-initialized projectId={}", projectId);
            return app;
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "notification.push", name = "enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private InputStream resolveCredentialsStream() throws IOException {
        if (serviceAccountBase64 != null && !serviceAccountBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountBase64.getBytes(StandardCharsets.UTF_8));
            return new ByteArrayInputStream(decoded);
        }

        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            return new FileInputStream(serviceAccountPath);
        }

        throw new IllegalStateException("Push habilitado, mas nenhuma credencial Firebase foi configurada.");
    }
}
