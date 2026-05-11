package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.FixedExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FixedExpenseResponseDTO(
    UUID id,
    FixedExpenseCategory category,
    String categoryLabel,
    String customName,
    BigDecimal amount,
    Integer month,
    Integer year,
    LocalDateTime createdAt
) {}
