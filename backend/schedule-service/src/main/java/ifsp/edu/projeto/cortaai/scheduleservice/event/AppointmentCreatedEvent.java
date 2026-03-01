package ifsp.edu.projeto.cortaai.scheduleservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
        UUID appointmentId,
        UUID customerId,
        UUID barberId,
        UUID barbershopId,
        String customerName,
        String barberName,
        String barbershopName,
        LocalDateTime startTime,
        BigDecimal totalPrice
) {}

