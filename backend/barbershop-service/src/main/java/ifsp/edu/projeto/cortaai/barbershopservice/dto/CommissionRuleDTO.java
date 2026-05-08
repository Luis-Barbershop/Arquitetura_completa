package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRuleDTO(
        UUID id,
        UUID activityId,
        String activityName,
        BigDecimal percentage
) {}
