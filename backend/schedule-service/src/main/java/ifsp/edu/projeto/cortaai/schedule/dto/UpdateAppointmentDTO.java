package ifsp.edu.projeto.cortaai.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateAppointmentDTO {

    private UUID barberId;

    private OffsetDateTime startTime;

    private List<UUID> activityIds;

    private String notes;
}
