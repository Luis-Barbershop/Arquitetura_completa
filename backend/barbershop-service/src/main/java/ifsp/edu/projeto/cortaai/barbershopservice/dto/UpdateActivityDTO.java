package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateActivityDTO {

    @Size(max = 255)
    private String activityName;

    @Positive
    private BigDecimal price;

    @Positive
    private Integer durationMinutes;
}

