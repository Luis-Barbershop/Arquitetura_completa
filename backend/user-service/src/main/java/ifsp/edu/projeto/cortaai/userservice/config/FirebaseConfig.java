package ifsp.edu.projeto.cortaai.userservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    /**
     * JSON completo da service account. Em produção, injete via variável de ambiente
     * FIREBASE_SERVICE_ACCOUNT_JSON. Em dev/teste, pode montar o arquivo e usar
     * GOOGLE_APPLICATION_CREDENTIALS apontando para o caminho.
     */
    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp já inicializado, reutilizando instância existente.");
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options;

        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            // Caminho 1: JSON embutido via env var (produção / Docker)
            log.info("Inicializando Firebase com credenciais JSON da variável de ambiente.");
            InputStream credentialsStream = new ByteArrayInputStream(
                    serviceAccountJson.getBytes(StandardCharsets.UTF_8));
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
        } else {
            // Caminho 2: Application Default Credentials
            // (GOOGLE_APPLICATION_CREDENTIALS=<path-to-key.json> ou conta de serviço do GCP)
            log.info("Inicializando Firebase com Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS).");
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
        }

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
