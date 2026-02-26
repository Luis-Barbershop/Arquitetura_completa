package ifsp.edu.projeto.cortaai.barbershop.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBarbershopDTO {

    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String name;

    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
    private String address;
}
