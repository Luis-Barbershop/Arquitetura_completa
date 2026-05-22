package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.model.Product;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import ifsp.edu.projeto.cortaai.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockAnalyticsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private StockAnalyticsService service;

    @Test
    void shouldMapStockHealthAlertsAndRestockFlag() {
        UUID shopId = UUID.randomUUID();
        UUID lowProductId = UUID.randomUUID();
        UUID healthyProductId = UUID.randomUUID();
        Product lowStock = product(lowProductId, "Pomada", ProductCategory.OTHER, 2, 2);
        Product healthy = product(healthyProductId, "Shampoo", ProductCategory.SHAMPOO, 8, 3);

        when(productRepository.findStockHealthByBarbershopId(shopId))
                .thenReturn(List.of(lowStock, healthy));

        List<StockHealthAlertResponseDTO> alerts = service.getStockHealthAlert(shopId.toString());

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).productId()).isEqualTo(lowProductId.toString());
        assertThat(alerts.get(0).productName()).isEqualTo("Pomada");
        assertThat(alerts.get(0).category()).isEqualTo("OTHER");
        assertThat(alerts.get(0).currentStock()).isEqualTo(2);
        assertThat(alerts.get(0).predictedMinimum()).isEqualTo(2);
        assertThat(alerts.get(0).requiresRestock()).isTrue();
        assertThat(alerts.get(1).requiresRestock()).isFalse();
    }

    @Test
    void shouldHandleProductsWithoutCategory() {
        UUID shopId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product uncategorized = product(productId, "Produto customizado", null, 1, 5);

        when(productRepository.findStockHealthByBarbershopId(shopId)).thenReturn(List.of(uncategorized));

        List<StockHealthAlertResponseDTO> alerts = service.getStockHealthAlert(shopId.toString());

        assertThat(alerts).singleElement().satisfies(alert -> {
            assertThat(alert.productId()).isEqualTo(productId.toString());
            assertThat(alert.category()).isNull();
            assertThat(alert.requiresRestock()).isTrue();
        });
    }

    private Product product(UUID id, String name, ProductCategory category, int stockQuantity, int minStockQuantity) {
        return Product.builder()
                .id(id)
                .barbershopId(UUID.randomUUID())
                .name(name)
                .price(BigDecimal.TEN)
                .category(category)
                .stockQuantity(stockQuantity)
                .minStockQuantity(minStockQuantity)
                .active(true)
                .build();
    }
}
