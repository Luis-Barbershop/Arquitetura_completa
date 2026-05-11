package ifsp.edu.projeto.cortaai.productservice.repository.analytics;

import ifsp.edu.projeto.cortaai.productservice.model.analytics.VStockHealthAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VStockHealthAlertRepository extends JpaRepository<VStockHealthAlert, String> {

    /**
     * Retorna todos os produtos ativos filtrando por barbearia.
     * A view v_stock_health_alert já é baseada em products (active=1); filtramos por barbershop_id.
     */
    @Query(value = """
            SELECT
                v.product_id          AS productId,
                v.product_name        AS productName,
                v.category            AS category,
                v.current_stock       AS currentStock,
                v.predicted_minimum   AS predictedMinimum,
                v.requires_restock    AS requiresRestock
            FROM v_stock_health_alert v
            INNER JOIN products p ON p.id = v.product_id
            WHERE p.barbershop_id = :barbershopId
            ORDER BY v.requires_restock DESC, v.current_stock ASC
            """, nativeQuery = true)
    List<StockHealthAlertProjection> findByBarbershopId(String barbershopId);

    interface StockHealthAlertProjection {
        String getProductId();
        String getProductName();
        String getCategory();
        Integer getCurrentStock();
        Integer getPredictedMinimum();
        Integer getRequiresRestock();
    }
}
