package ifsp.edu.projeto.cortaai.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentReminderEvent {
    private UUID appointmentId;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String barbershopName;
    private String barberName;
    private LocalDateTime startTime;
}
