package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;

import java.math.BigDecimal;

/**
 * DTO para atualizar um produto.
 */
public record UpdateProductDTO(
        String name,
        String description,
        BigDecimal price,
        ProductCategory category,
        Integer stockQuantity,
        String imageUrl,
        Boolean active
) {}
