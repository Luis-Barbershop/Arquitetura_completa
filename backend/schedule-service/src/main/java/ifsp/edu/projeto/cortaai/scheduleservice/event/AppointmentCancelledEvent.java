package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
        UUID appointmentId,
        UUID customerId,
        UUID barberId,
        String cancelledBy,
        String customerName,
        String customerEmail,
        String barberName,
        String barberEmail,
        String barbershopName,
        LocalDateTime startTime
) {}

