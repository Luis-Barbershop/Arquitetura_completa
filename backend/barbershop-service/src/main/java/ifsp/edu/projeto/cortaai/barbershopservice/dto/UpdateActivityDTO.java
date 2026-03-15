package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateActivityDTO {

    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s'.\\-&]*$", message = "Nome da atividade contém caracteres inválidos")
    private String activityName;

    @Positive
    private BigDecimal price;

    @Positive
    private Integer durationMinutes;
}

