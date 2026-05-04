package ifsp.edu.projeto.cortaai.barbershopservice.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CPFValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }

        if (!cpf.matches("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}")) {
            return false;
        }

        String digits = cpf.replaceAll("\\D", "");
        if (digits.chars().distinct().count() == 1) {
            return false;
        }

        int firstDigit = calculateDigit(digits, 9, 10);
        int secondDigit = calculateDigit(digits, 10, 11);

        return firstDigit == Character.digit(digits.charAt(9), 10)
                && secondDigit == Character.digit(digits.charAt(10), 10);
    }

    private int calculateDigit(String cpf, int length, int weight) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.digit(cpf.charAt(i), 10) * weight--;
        }

        int digit = 11 - (sum % 11);
        return digit > 9 ? 0 : digit;
    }
}
