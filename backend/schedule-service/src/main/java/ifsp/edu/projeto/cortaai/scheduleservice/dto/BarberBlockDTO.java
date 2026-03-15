package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class BarberBlockDTO {
    private UUID id;
    private UUID barberId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private LocalDateTime dateCreated;
}