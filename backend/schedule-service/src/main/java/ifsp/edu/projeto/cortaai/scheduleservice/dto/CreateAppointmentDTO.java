package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateAppointmentDTO {

    private UUID customerId;

    @NotNull
    private UUID barberId;

    @NotNull
    private UUID barbershopId;

    @NotNull
    private List<UUID> activityIds;

    @NotNull
    private LocalDateTime startTime;
}

