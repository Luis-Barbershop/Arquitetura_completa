package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
}
