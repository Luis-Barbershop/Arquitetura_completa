package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentConcludedEvent(
        UUID appointmentId,
        UUID customerId,
        UUID barberId,
        UUID barbershopId,
        String customerName,
        String customerEmail,
        String barberName,
        String barbershopName,
        LocalDateTime startTime
) {}

