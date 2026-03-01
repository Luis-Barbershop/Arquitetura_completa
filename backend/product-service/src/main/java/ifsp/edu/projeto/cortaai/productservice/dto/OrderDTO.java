package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de retorno de pedido.
 */
public record OrderDTO(
        UUID id,
        UUID customerId,
        UUID barbershopId,
        OrderStatus status,
        BigDecimal totalPrice,
        List<OrderItemDTO> items,
        LocalDateTime createdAt
) {}
