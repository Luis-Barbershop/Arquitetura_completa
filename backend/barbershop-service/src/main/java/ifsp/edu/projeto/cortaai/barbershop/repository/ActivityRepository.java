package ifsp.edu.projeto.cortaai.barbershop.repository;

import ifsp.edu.projeto.cortaai.barbershop.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    
    List<Activity> findByBarbershopId(UUID barbershopId);
    
    List<Activity> findByIdIn(List<UUID> ids);
}
