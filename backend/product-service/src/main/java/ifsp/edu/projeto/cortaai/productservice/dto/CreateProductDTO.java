package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para criar um produto.
 */
public record CreateProductDTO(
        @NotNull UUID barbershopId,
        @NotBlank String name,
        String description,
        @NotNull @Positive BigDecimal price,
        ProductCategory category,
        Integer stockQuantity,
        String imageUrl
) {}
