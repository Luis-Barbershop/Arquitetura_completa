package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloseBarbershopRequestDTO {
    @NotBlank(message = "A senha é obrigatória para fechar a barbearia.")
    private String password;
}

