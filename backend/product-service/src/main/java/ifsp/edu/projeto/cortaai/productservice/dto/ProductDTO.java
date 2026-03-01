package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de retorno de produto.
 */
public record ProductDTO(
        UUID id,
        UUID barbershopId,
        String name,
        String description,
        BigDecimal price,
        ProductCategory category,
        Integer stockQuantity,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt
) {}
