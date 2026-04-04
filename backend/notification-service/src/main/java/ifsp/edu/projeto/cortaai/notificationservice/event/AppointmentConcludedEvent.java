package ifsp.edu.projeto.cortaai.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento recebido quando um agendamento é concluído.
 * Publicado pelo schedule-service (Dev 1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentConcludedEvent {
    private UUID appointmentId;
    private UUID customerId;
    private UUID barberId;
    private UUID barbershopId;
    private String customerName;
    private String customerEmail;
    private String barberName;
    private String barbershopName;
    private LocalDateTime startTime;
}
