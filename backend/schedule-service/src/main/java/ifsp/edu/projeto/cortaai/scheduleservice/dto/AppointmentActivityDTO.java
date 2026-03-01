package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class AppointmentActivityDTO {
    private UUID id;
    private UUID activityId;
    private String activityName;
    private BigDecimal price;
    private Integer durationMinutes;
}

