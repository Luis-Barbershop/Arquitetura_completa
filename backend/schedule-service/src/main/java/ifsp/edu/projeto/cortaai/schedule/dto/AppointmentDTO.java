package ifsp.edu.projeto.cortaai.schedule.dto;

import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AppointmentDTO {
    private Long id;
    private UUID barbershopId;
    private UUID barberId;
    private UUID customerId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private AppointmentStatus status;
    private List<UUID> activityIds;
    private String notes;
    private OffsetDateTime dateCreated;
    private OffsetDateTime lastUpdated;

    // Additional information fetched from other services
    private String barbershopName;
    private String barberName;
    private String customerName;
}
