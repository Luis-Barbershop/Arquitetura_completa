package ifsp.edu.projeto.cortaai.productservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Item individual dentro de um pedido (request).
 */
public record OrderItemRequestDTO(
        @NotNull UUID productId,
        @NotNull @Positive Integer quantity
) {}
