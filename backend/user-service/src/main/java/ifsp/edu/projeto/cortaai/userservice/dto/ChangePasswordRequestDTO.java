package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDTO(

        @NotBlank(message = "idToken é obrigatório")
        String idToken,

        @NotBlank(message = "newPassword é obrigatório")
        @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres")
        String newPassword
) {
}
