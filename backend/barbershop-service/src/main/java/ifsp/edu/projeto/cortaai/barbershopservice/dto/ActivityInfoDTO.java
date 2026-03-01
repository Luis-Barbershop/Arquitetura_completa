package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO inter-serviço — consumido pelo schedule-service via Feign.
 */
public record ActivityInfoDTO(
        UUID id,
        String activityName,
        BigDecimal price,
        Integer durationMinutes,
        UUID barbershopId
) {}

