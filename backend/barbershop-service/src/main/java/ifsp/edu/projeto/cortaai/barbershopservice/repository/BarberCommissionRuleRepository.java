package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarberCommissionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarberCommissionRuleRepository extends JpaRepository<BarberCommissionRule, UUID> {

    List<BarberCommissionRule> findByBarbershopIdAndBarberId(UUID barbershopId, UUID barberId);

    List<BarberCommissionRule> findByBarbershopId(UUID barbershopId);

    Optional<BarberCommissionRule> findByIdAndBarbershopIdAndBarberId(UUID id, UUID barbershopId, UUID barberId);

    Optional<BarberCommissionRule> findByBarbershopIdAndBarberIdAndActivityId(UUID barbershopId, UUID barberId, UUID activityId);
}
