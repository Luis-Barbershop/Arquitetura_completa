package ifsp.edu.projeto.cortaai.barbershopservice.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CNPJValidatorTest {

    private final CNPJValidator validator = new CNPJValidator();

    @Test
    void acceptsValidCnpjWithOrWithoutMask() {
        assertThat(validator.isValid("11222333000181", null)).isTrue();
        assertThat(validator.isValid("11.222.333/0001-81", null)).isTrue();
    }

    @Test
    void rejectsInvalidCnpj() {
        assertThat(validator.isValid("11222333000182", null)).isFalse();
        assertThat(validator.isValid("11111111111111", null)).isFalse();
        assertThat(validator.isValid("1122233300018a", null)).isFalse();
    }
}
