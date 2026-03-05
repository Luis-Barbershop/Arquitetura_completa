package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    @Query("SELECT a FROM Activity a WHERE a.barbershop.id = :barbershopId")
    List<Activity> findByBarbershopId(@Param("barbershopId") UUID barbershopId);
    
}