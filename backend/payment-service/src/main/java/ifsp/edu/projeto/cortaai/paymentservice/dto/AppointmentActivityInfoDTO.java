package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AppointmentActivityInfoDTO(
        UUID id,
        UUID activityId,
        String activityName,
        BigDecimal price,
        Integer durationMinutes
) {}
