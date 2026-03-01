package ifsp.edu.projeto.cortaai.productservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de retorno de item de pedido.
 */
public record OrderItemDTO(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal price,
        Integer quantity
) {}
