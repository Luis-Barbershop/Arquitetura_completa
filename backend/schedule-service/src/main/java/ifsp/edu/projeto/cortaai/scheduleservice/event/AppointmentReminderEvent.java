package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentReminderEvent(
        UUID appointmentId,
        UUID customerId,
        String customerName,
        String customerEmail,
        UUID barberId,
        String barberEmail,
        String barbershopName,
        String barberName,
        LocalDateTime startTime
) {}
