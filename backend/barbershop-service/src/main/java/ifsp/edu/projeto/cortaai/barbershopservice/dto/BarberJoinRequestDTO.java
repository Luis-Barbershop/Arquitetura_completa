package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import ifsp.edu.projeto.cortaai.barbershopservice.validator.CNPJ;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BarberJoinRequestDTO {
    @NotBlank
    @Size(min = 14, max = 18, message = "CNPJ deve ter entre 14 e 18 caracteres")
    @CNPJ
    private String cnpj;
}
