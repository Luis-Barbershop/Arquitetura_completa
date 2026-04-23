package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentRescheduledEvent(
        UUID appointmentId,
        UUID customerId,
        UUID barberId,
        UUID barbershopId,
        String customerName,
        String customerEmail,
        String barberName,
        String barberEmail,
        String barbershopName,
        LocalDateTime oldStartTime,
        LocalDateTime newStartTime,
        LocalDateTime newEndTime,
        String rescheduledBy
) {
}