package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivityDTO {
    private UUID id;
    private String activityName;
    private BigDecimal price;
    private Integer durationMinutes;
    private UUID barbershopId;
    private String imageUrl;
}

