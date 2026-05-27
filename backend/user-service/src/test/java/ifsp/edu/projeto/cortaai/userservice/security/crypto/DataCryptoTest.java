package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DataCryptoTest {

    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeAll
    static void configure() {
        DataCrypto.configure(TEST_KEY);
    }

    @Test
    void encryptsAndDecryptsSensitiveValue() {
        String encrypted = DataCrypto.encrypt("52998224725");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("52998224725");
        assertThat(DataCrypto.decrypt(encrypted)).isEqualTo("52998224725");
    }

    @Test
    void encryptionIsDeterministicForRepositoryLookups() {
        assertThat(DataCrypto.encrypt("52998224725")).isEqualTo(DataCrypto.encrypt("52998224725"));
    }

    @Test
    void keepsLegacyPlainTextReadable() {
        assertThat(DataCrypto.decrypt("52998224725")).isEqualTo("52998224725");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldThrowWhenNotConfigured() {
        AtomicReference<SecretKeySpec> ref =
                (AtomicReference<SecretKeySpec>) ReflectionTestUtils.getField(DataCrypto.class, "keySpec");
        SecretKeySpec saved = ref.getAndSet(null);
        try {
            assertThatThrownBy(() -> DataCrypto.encrypt("valor-secreto"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inicializada");
        } finally {
            ref.set(saved);
        }
    }

    @Test
    void shouldComputeHmacSha256() {
        String hash = DataCrypto.hmacSha256("52998224725");

        assertThat(hash).isNotNull();
        assertThat(hash).isNotBlank();
        // Determinístico: mesma entrada, mesmo hash
        assertThat(DataCrypto.hmacSha256("52998224725")).isEqualTo(hash);
        // Entradas distintas produzem hashes distintos
        assertThat(DataCrypto.hmacSha256("00000000000")).isNotEqualTo(hash);
    }
}
