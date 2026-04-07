package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBarbershopDTO {

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s'.\\-&]+$", message = "Nome da barbearia contém caracteres inválidos")
    private String name;

    @NotBlank
    @Size(min = 14, max = 14)
    @Pattern(regexp = "^[0-9]{14}$", message = "CNPJ deve conter exatamente 14 dígitos numéricos")
    private String cnpj;

    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s,.°ºª\\-/:()'\"+]+$", message = "Endereço contém caracteres inválidos")
    private String address;
}

