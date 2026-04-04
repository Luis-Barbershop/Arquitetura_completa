package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.MovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementDTO(
        UUID id,
        UUID productId,
        MovementType type,
        Integer quantity,
        String reason,
        LocalDateTime createdAt
) {}

