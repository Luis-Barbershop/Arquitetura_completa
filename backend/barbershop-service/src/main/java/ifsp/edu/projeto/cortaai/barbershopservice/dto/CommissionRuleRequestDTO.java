package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRuleRequestDTO(
        @NotNull UUID activityId,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal percentage
) {}
