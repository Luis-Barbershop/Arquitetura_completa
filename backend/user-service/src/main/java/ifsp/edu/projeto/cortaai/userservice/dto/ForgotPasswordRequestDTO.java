package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email
) {
}
