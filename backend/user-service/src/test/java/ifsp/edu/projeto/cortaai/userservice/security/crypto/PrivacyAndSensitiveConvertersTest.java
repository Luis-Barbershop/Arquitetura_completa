package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PrivacyAndSensitiveConvertersTest {

    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeAll
    static void configure() {
        DataCrypto.configure(TEST_KEY);
    }

    @Test
    void shouldNormalizeEmailsBeforeHashing() {
        assertThat(PrivacyHash.normalizeEmail("  CLIENTE@CortaAI.COM  "))
                .isEqualTo("cliente@cortaai.com");
        assertThat(PrivacyHash.normalizeEmail("   ")).isNull();
        assertThat(PrivacyHash.normalizeEmail(null)).isNull();
    }

    @Test
    void shouldGenerateStableEmailHashFromNormalizedEmail() {
        String first = PrivacyHash.emailHash("CLIENTE@CortaAI.COM");
        String second = PrivacyHash.emailHash(" cliente@cortaai.com ");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
        assertThat(first).doesNotContain("cliente");
    }

    @Test
    void shouldEncryptAndDecryptSensitiveStrings() {
        SensitiveStringConverter converter = new SensitiveStringConverter();

        String stored = converter.convertToDatabaseColumn("cliente@cortaai.com");

        assertThat(stored).startsWith("enc:v1:");
        assertThat(stored).doesNotContain("cliente@cortaai.com");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo("cliente@cortaai.com");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute("legado")).isEqualTo("legado");
    }

    @Test
    void shouldEncryptAndDecryptSensitiveLocalDates() {
        SensitiveLocalDateConverter converter = new SensitiveLocalDateConverter();
        LocalDate birthDate = LocalDate.of(1999, 5, 21);

        String stored = converter.convertToDatabaseColumn(birthDate);

        assertThat(stored).startsWith("enc:v1:");
        assertThat(stored).doesNotContain("1999-05-21");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(birthDate);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute("")).isNull();
    }
}
