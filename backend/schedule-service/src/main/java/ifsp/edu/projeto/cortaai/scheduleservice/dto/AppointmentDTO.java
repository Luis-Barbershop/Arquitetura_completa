package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AppointmentDTO {
    private UUID id;
    private UUID customerId;
    private UUID barberId;
    private UUID barbershopId;
    private String customerName;
    private String barberName;
    private String barbershopName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalPrice;
    private String status;
    private List<AppointmentActivityDTO> activities;
    private LocalDateTime dateCreated;
}