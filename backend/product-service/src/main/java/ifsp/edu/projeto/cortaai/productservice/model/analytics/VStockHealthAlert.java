package ifsp.edu.projeto.cortaai.productservice.model.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_stock_health_alert")
@Getter
public class VStockHealthAlert {

    @Id
    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "category")
    private String category;

    @Column(name = "current_stock")
    private Integer currentStock;

    @Column(name = "predicted_minimum")
    private Integer predictedMinimum;

    @Column(name = "requires_restock")
    private Integer requiresRestock;
}
