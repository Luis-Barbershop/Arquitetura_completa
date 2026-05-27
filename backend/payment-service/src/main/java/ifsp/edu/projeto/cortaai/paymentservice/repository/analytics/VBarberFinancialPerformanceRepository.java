package ifsp.edu.projeto.cortaai.paymentservice.repository.analytics;

import ifsp.edu.projeto.cortaai.paymentservice.model.analytics.VBarberFinancialPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VBarberFinancialPerformanceRepository extends JpaRepository<VBarberFinancialPerformance, String> {
  
}