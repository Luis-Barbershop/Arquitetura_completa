package ifsp.edu.projeto.cortaai.productservice.dto;

import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO para atualizar um produto.
 */
public record UpdateProductDTO(
        @Size(max = 255)
        @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s'.\\-&]*$", message = "Nome do produto contém caracteres inválidos")
        String name,

        @Size(max = 1000)
        String description,

        @Positive
        BigDecimal price,

        ProductCategory category,

        Integer stockQuantity,

        @Size(max = 500)
        @Pattern(regexp = "^(https?://.*)?$", message = "URL da imagem inválida")
        String imageUrl,

        Boolean active
) {}
