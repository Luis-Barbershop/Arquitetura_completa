package ifsp.edu.projeto.cortaai.productservice.repository.analytics;

import ifsp.edu.projeto.cortaai.productservice.model.analytics.VStockHealthAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VStockHealthAlertRepository extends JpaRepository<VStockHealthAlert, String> {
    List<VStockHealthAlert> findByBarbershopId(String barbershopId);
}