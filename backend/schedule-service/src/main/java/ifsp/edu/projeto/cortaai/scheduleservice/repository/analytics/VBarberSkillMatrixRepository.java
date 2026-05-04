package ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics;

import ifsp.edu.projeto.cortaai.scheduleservice.model.analytics.VBarberSkillMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VBarberSkillMatrixRepository extends JpaRepository<VBarberSkillMatrix, VBarberSkillMatrix.VBarberSkillMatrixId> {

    @Query(value = """
            SELECT DISTINCT v.barber_id, v.barber_name, v.activity_name,
                            v.times_executed, v.total_generated_by_activity
            FROM v_barber_skill_matrix v
            WHERE v.barber_id IN (
                SELECT DISTINCT a.barber_id FROM appointments a
                WHERE a.barbershop_id = :barbershopId
            )
            """, nativeQuery = true)
    List<VBarberSkillMatrix> findByBarbershopId(@Param("barbershopId") String barbershopId);
}
