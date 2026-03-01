package ifsp.edu.projeto.cortaai.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Evento recebido quando um agendamento é cancelado.
 * Publicado pelo schedule-service (Dev 1).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCancelledEvent {
    private UUID appointmentId;
    private UUID customerId;
    private UUID barberId;
    private String cancelledBy; // "CUSTOMER" ou "BARBER"
}
