package ifsp.edu.projeto.cortaai.productservice.repository.analytics;

import ifsp.edu.projeto.cortaai.productservice.model.analytics.VStockHealthAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VStockHealthAlertRepository extends JpaRepository<VStockHealthAlert, String> {
}