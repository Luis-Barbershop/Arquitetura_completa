package ifsp.edu.projeto.cortaai.paymentservice.repository.analytics;

import ifsp.edu.projeto.cortaai.paymentservice.model.analytics.VBarberFinancialPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VBarberFinancialPerformanceRepository extends JpaRepository<VBarberFinancialPerformance, String> {
    // Retorna todos os registros — filtro de barbershop feito na view via barbershop_id se necessário
    // A view já agrega por barber, sem filtro de shop (multi-barbershop não implementado ainda)
}
