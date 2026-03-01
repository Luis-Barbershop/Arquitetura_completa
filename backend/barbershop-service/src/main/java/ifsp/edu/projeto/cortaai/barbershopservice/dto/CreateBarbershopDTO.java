package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBarbershopDTO {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    @Size(min = 14, max = 14)
    private String cnpj;

    @Size(max = 255)
    private String address;
}

