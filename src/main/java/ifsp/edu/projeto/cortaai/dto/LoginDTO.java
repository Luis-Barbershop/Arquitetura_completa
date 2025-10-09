package ifsp.edu.projeto.cortaai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    @NotNull
    @Size(max = 70)
    private String email;

    @NotNull
    @Size(max = 70)
    private String password;
}