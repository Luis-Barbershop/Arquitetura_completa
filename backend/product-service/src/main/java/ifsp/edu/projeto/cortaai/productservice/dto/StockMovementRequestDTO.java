package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record StockMovementRequestDTO(
        @NotNull UUID productId,
        @NotNull MovementType type,
        @NotNull @Positive Integer quantity,
        BigDecimal unitSalePrice,
        String notes
) {}
