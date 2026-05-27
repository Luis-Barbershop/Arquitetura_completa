package ifsp.edu.projeto.cortaai.productservice.repository.analytics;

import ifsp.edu.projeto.cortaai.productservice.model.analytics.VStockHealthAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VStockHealthAlertRepository extends JpaRepository<VStockHealthAlert, String> {

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