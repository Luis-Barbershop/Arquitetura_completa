package ifsp.edu.projeto.cortaai.barbershopservice.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CPFValidatorTest {

    private final CPFValidator validator = new CPFValidator();

    @Test
    void acceptsValidCpfWithOrWithoutMask() {
        assertThat(validator.isValid("52998224725", null)).isTrue();
        assertThat(validator.isValid("529.982.247-25", null)).isTrue();
    }

    @Test
    void rejectsInvalidCpf() {
        assertThat(validator.isValid("52998224726", null)).isFalse();
        assertThat(validator.isValid("11111111111", null)).isFalse();
        assertThat(validator.isValid("5299822472a", null)).isFalse();
    }
}
