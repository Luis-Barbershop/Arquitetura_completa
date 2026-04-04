package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO para criar um produto.
 */
public record CreateProductDTO(
        @NotNull UUID barbershopId,

        @NotBlank
        @Size(max = 255)
        @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s'.\\-&]+$", message = "Nome do produto contém caracteres inválidos")
        String name,

        @Size(max = 1000)
        String description,

        @NotNull @Positive BigDecimal price,

        ProductCategory category,

        Integer stockQuantity,

        Integer minStockQuantity,

        @Size(max = 500)
        @Pattern(regexp = "^(https?://.*)?$", message = "URL da imagem inválida")
        String imageUrl
) {}
