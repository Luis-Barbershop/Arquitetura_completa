package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryProductItemDTO(
        UUID id,
        String name,
        UUID categoryId,
        String categoryName,
        ProductCategory category,
        BigDecimal price,
        Integer stockQuantity,
        Integer minStockQuantity,
        boolean lowStock,
        boolean active
) {}
