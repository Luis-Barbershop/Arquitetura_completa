package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.FixedExpenseCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FixedExpenseRequestDTO(
    @NotNull FixedExpenseCategory category,
    String customName,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull @Min(1) @Max(12) Integer month,
    @NotNull @Min(2000) Integer year,
    Boolean recurringMonthly
) {}
