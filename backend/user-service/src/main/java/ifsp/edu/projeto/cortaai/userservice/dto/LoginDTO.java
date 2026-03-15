package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    @NotNull
    @Size(max = 70)
    @Pattern(regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$", message = "Formato de e-mail inválido")
    private String email;

    @NotNull
    @Size(max = 70)
    private String password;
}

