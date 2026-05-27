package ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics;

import ifsp.edu.projeto.cortaai.scheduleservice.model.analytics.VBarberSkillMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VBarberSkillMatrixRepository extends JpaRepository<VBarberSkillMatrix, String> {
    List<VBarberSkillMatrix> findByBarbershopId(String barbershopId);
}