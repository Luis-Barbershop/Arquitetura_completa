package ifsp.edu.projeto.cortaai.userservice.validator;

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

        cpf = cpf.replaceAll("\\D", "");

        if (cpf.equals("00000000000") || cpf.equals("11111111111") ||
                cpf.equals("22222222222") || cpf.equals("33333333333") ||
                cpf.equals("44444444444") || cpf.equals("55555555555") ||
                cpf.equals("66666666666") || cpf.equals("77777777777") ||
                cpf.equals("88888888888") || cpf.equals("99999999999") ||
                (cpf.length() != 11)) {
            return false;
        }

        int dig10 = calculateDigit(cpf, 9, 10);
        int dig11 = calculateDigit(cpf, 10, 11);

        return dig10 == Character.digit(cpf.charAt(9), 10)
                && dig11 == Character.digit(cpf.charAt(10), 10);
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
