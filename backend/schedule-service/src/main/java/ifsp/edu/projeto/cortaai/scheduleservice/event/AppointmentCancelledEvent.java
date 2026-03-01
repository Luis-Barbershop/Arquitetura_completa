package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.util.UUID;

public record AppointmentCancelledEvent(
        UUID appointmentId,
        UUID customerId,
        UUID barberId,
        String cancelledBy
) {}

