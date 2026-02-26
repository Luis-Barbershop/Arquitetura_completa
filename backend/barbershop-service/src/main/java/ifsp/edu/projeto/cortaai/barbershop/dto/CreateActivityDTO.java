package ifsp.edu.projeto.cortaai.barbershop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateActivityDTO {

    @NotBlank(message = "Nome da atividade é obrigatório")
    @Size(max = 255, message = "Nome da atividade deve ter no máximo 255 caracteres")
    private String activityName;

    @NotNull(message = "Preço é obrigatório")
    @Positive(message = "Preço deve ser positivo")
    private BigDecimal price;

    @NotNull(message = "Duração é obrigatória")
    @Positive(message = "Duração deve ser positiva")
    private Integer durationMinutes;
}
