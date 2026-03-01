package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBarbershopDTO {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String address;
}

