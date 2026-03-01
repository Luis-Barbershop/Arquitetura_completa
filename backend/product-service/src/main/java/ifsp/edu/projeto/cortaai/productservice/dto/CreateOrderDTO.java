package ifsp.edu.projeto.cortaai.productservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * DTO para criar um pedido.
 */
public record CreateOrderDTO(
        @NotNull UUID barbershopId,
        @NotEmpty @Valid List<OrderItemRequestDTO> items
) {}
