package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.util.UUID;

public record AppointmentConcludedEvent(
        UUID appointmentId,
        UUID customerId,
        UUID barberId,
        UUID barbershopId
) {}

