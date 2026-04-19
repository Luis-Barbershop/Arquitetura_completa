package ifsp.edu.projeto.cortaai.paymentservice.repository;

import ifsp.edu.projeto.cortaai.paymentservice.model.DashboardKpiDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardKpiDailyRepository extends JpaRepository<DashboardKpiDaily, UUID> {

    Optional<DashboardKpiDaily> findByBarbershopIdAndReferenceDate(UUID barbershopId, LocalDate referenceDate);
}
