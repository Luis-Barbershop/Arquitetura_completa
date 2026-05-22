package ifsp.edu.projeto.cortaai.productservice.mapper;

import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.model.Category;
import ifsp.edu.projeto.cortaai.productservice.model.Product;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void shouldMapNullProductToNull() {
        assertThat(mapper.toDTO(null)).isNull();
    }

    @Test
    void shouldMapProductWithDynamicCategory() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 22, 8, 30);
        Product product = Product.builder()
                .id(productId)
                .barbershopId(shopId)
                .name("Pomada")
                .description("Forte")
                .price(new BigDecimal("35.00"))
                .dynamicCategory(Category.builder()
                        .id(categoryId)
                        .name("Finalizadores")
                        .barbershopId(shopId)
                        .build())
                .category(ProductCategory.POMADE)
                .stockQuantity(5)
                .minStockQuantity(2)
                .imageUrl("https://cdn.test/pomada.png")
                .active(true)
                .createdAt(createdAt)
                .build();

        ProductDTO dto = mapper.toDTO(product);

        assertThat(dto.id()).isEqualTo(productId);
        assertThat(dto.barbershopId()).isEqualTo(shopId);
        assertThat(dto.name()).isEqualTo("Pomada");
        assertThat(dto.categoryId()).isEqualTo(categoryId);
        assertThat(dto.categoryName()).isEqualTo("Finalizadores");
        assertThat(dto.category()).isEqualTo(ProductCategory.POMADE);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldMapProductWithoutDynamicCategory() {
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .barbershopId(UUID.randomUUID())
                .name("Shampoo")
                .price(BigDecimal.TEN)
                .category(ProductCategory.SHAMPOO)
                .stockQuantity(1)
                .minStockQuantity(0)
                .active(false)
                .build();

        ProductDTO dto = mapper.toDTO(product);

        assertThat(dto.categoryId()).isNull();
        assertThat(dto.categoryName()).isNull();
        assertThat(dto.active()).isFalse();
    }
}
