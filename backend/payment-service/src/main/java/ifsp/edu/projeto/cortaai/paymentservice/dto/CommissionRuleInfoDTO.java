package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRuleInfoDTO(
        UUID id,
        UUID activityId,
        String activityName,
        BigDecimal percentage
) {}
