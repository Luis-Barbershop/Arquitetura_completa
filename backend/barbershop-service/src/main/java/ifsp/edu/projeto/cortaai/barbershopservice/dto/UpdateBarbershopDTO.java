package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBarbershopDTO {

    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s'.\\-&]*$", message = "Nome da barbearia contém caracteres inválidos")
    private String name;

    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s,.°ºª\\-/]*$", message = "Endereço contém caracteres inválidos")
    private String address;
}

