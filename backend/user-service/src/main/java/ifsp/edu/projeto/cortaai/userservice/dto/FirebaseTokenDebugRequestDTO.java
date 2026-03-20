package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record FirebaseTokenDebugRequestDTO(
        @NotBlank(message = "idToken é obrigatório")
        String idToken
) {
}

