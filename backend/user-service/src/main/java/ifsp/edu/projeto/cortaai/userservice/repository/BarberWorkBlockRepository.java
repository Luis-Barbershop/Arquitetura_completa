package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.BarberWorkBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Repository
public interface BarberWorkBlockRepository extends JpaRepository<BarberWorkBlock, UUID> {

    List<BarberWorkBlock> findByBarberIdOrderByDayOfWeekAscStartTimeAsc(UUID barberId);

    List<BarberWorkBlock> findByBarberIdAndDayOfWeek(UUID barberId, DayOfWeek dayOfWeek);

    void deleteByBarberId(UUID barberId);
}
