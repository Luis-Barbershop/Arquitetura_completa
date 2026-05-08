package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementDTO(
        UUID id,
        UUID productId,
        MovementType type,
        Integer quantity,
        BigDecimal unitSalePrice,
        String notes,
        String reason,
        LocalDateTime createdAt
) {}
