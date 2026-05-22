package ifsp.edu.projeto.cortaai.notificationservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseAdminConfigTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldResolveCredentialsFromBase64BeforePath() throws Exception {
        FirebaseAdminConfig config = new FirebaseAdminConfig();
        String json = "{\"type\":\"service_account\"}";
        ReflectionTestUtils.setField(config, "serviceAccountBase64",
                Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(config, "serviceAccountPath", tempDir.resolve("ignored.json").toString());

        try (InputStream stream = ReflectionTestUtils.invokeMethod(config, "resolveCredentialsStream")) {
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
        }
    }

    @Test
    void shouldResolveCredentialsFromConfiguredPath() throws Exception {
        FirebaseAdminConfig config = new FirebaseAdminConfig();
        Path credentials = tempDir.resolve("firebase.json");
        Files.writeString(credentials, "{\"project_id\":\"test\"}", StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(config, "serviceAccountBase64", "");
        ReflectionTestUtils.setField(config, "serviceAccountPath", credentials.toString());

        try (InputStream stream = ReflectionTestUtils.invokeMethod(config, "resolveCredentialsStream")) {
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).contains("project_id");
        }
    }

    @Test
    void shouldFailWhenPushIsEnabledWithoutCredentials() {
        FirebaseAdminConfig config = new FirebaseAdminConfig();
        ReflectionTestUtils.setField(config, "serviceAccountBase64", "");
        ReflectionTestUtils.setField(config, "serviceAccountPath", "");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(config, "resolveCredentialsStream"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nenhuma credencial Firebase");
    }
}
