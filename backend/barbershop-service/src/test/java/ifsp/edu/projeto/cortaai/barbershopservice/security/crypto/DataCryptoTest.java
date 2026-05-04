package ifsp.edu.projeto.cortaai.barbershopservice.security.crypto;

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
        String encrypted = DataCrypto.encrypt("11222333000181");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("11222333000181");
        assertThat(DataCrypto.decrypt(encrypted)).isEqualTo("11222333000181");
    }

    @Test
    void encryptionIsDeterministicForRepositoryLookups() {
        assertThat(DataCrypto.encrypt("11222333000181")).isEqualTo(DataCrypto.encrypt("11222333000181"));
    }

    @Test
    void keepsLegacyPlainTextReadable() {
        assertThat(DataCrypto.decrypt("11222333000181")).isEqualTo("11222333000181");
    }
}
