package ifsp.edu.projeto.cortaai.barbershop.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateActivityDTO {

    @Size(max = 255, message = "Nome da atividade deve ter no máximo 255 caracteres")
    private String activityName;

    @Positive(message = "Preço deve ser positivo")
    private BigDecimal price;

    @Positive(message = "Duração deve ser positiva")
    private Integer durationMinutes;
}
