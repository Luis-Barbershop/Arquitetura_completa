package ifsp.edu.projeto.cortaai.paymentservice.repository.analytics;

import ifsp.edu.projeto.cortaai.paymentservice.model.analytics.VBarberFinancialPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VBarberFinancialPerformanceRepository extends JpaRepository<VBarberFinancialPerformance, String> {
    List<VBarberFinancialPerformance> findByBarbershopId(UUID barbershopId);
}