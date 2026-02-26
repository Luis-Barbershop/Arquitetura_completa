package ifsp.edu.projeto.cortaai.barbershop.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class ActivityDTO {
    private UUID id;
    private UUID barbershopId;
    private String activityName;
    private BigDecimal price;
    private Integer durationMinutes;
    private String imageUrl;
    private OffsetDateTime dateCreated;
    private OffsetDateTime lastUpdated;
}
