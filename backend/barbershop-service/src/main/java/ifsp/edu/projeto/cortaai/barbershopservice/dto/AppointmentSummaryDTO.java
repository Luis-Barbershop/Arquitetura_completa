package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentSummaryDTO(
        UUID id,
        UUID barberId,
        UUID customerId,
        UUID barbershopId,
        String customerName,
        String barberName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
) {}
