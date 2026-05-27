package ifsp.edu.projeto.cortaai.scheduleservice.security.crypto;

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
        String encrypted = DataCrypto.encrypt("Maria Silva");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("Maria Silva");
        assertThat(DataCrypto.decrypt(encrypted)).isEqualTo("Maria Silva");
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
}
