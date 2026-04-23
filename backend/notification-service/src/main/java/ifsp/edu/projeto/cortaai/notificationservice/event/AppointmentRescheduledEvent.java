package ifsp.edu.projeto.cortaai.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRescheduledEvent {
    private UUID appointmentId;
    private UUID customerId;
    private UUID barberId;
    private UUID barbershopId;
    private String customerName;
    private String customerEmail;
    private String barberName;
    private String barberEmail;
    private String barbershopName;
    private LocalDateTime oldStartTime;
    private LocalDateTime newStartTime;
    private LocalDateTime newEndTime;
    private String rescheduledBy;
}