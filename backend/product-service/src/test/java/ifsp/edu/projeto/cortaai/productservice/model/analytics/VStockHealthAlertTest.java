package ifsp.edu.projeto.cortaai.productservice.model.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class VStockHealthAlertTest {

    @Test
    void shouldExposeViewColumnsThroughGetters() {
        VStockHealthAlert alert = new VStockHealthAlert();
        ReflectionTestUtils.setField(alert, "productId", "product-1");
        ReflectionTestUtils.setField(alert, "productName", "Pomada");
        ReflectionTestUtils.setField(alert, "category", "OTHER");
        ReflectionTestUtils.setField(alert, "currentStock", 2);
        ReflectionTestUtils.setField(alert, "predictedMinimum", 5);
        ReflectionTestUtils.setField(alert, "requiresRestock", 1);

        assertThat(alert.getProductId()).isEqualTo("product-1");
        assertThat(alert.getProductName()).isEqualTo("Pomada");
        assertThat(alert.getCategory()).isEqualTo("OTHER");
        assertThat(alert.getCurrentStock()).isEqualTo(2);
        assertThat(alert.getPredictedMinimum()).isEqualTo(5);
        assertThat(alert.getRequiresRestock()).isEqualTo(1);
    }
}
