package ifsp.edu.projeto.cortaai.barbershopservice.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CNPJValidator implements ConstraintValidator<CNPJ, String> {

    private static final int[] FIRST_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String cnpj, ConstraintValidatorContext context) {
        if (cnpj == null || cnpj.isBlank()) {
            return true;
        }

        if (!cnpj.matches("\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}")) {
            return false;
        }

        String digits = cnpj.replaceAll("\\D", "");
        if (digits.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateDigit(digits, FIRST_DIGIT_WEIGHTS);
        int secondDigit = calculateDigit(digits, SECOND_DIGIT_WEIGHTS);

        return firstDigit == Character.digit(digits.charAt(12), 10)
                && secondDigit == Character.digit(digits.charAt(13), 10);
    }

    private int calculateDigit(String cnpj, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += Character.digit(cnpj.charAt(i), 10) * weights[i];
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
