package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateActivityDTO {

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s'.\\-&]+$", message = "Nome da atividade contém caracteres inválidos")
    private String activityName;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotNull
    @Positive
    private Integer durationMinutes;
}

