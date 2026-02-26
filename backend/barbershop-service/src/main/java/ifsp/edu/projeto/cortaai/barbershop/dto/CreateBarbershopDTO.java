package ifsp.edu.projeto.cortaai.barbershop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBarbershopDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    private String name;

    @NotBlank(message = "CNPJ é obrigatório")
    @Size(min = 14, max = 14, message = "CNPJ deve ter 14 dígitos")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter apenas números")
    private String cnpj;

    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres")
    private String address;
}
