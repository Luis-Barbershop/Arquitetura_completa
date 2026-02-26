package ifsp.edu.projeto.cortaai.schedule.repository;

import ifsp.edu.projeto.cortaai.schedule.model.BarberWorkHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberWorkHoursRepository extends JpaRepository<BarberWorkHours, Long> {

    List<BarberWorkHours> findByBarberId(UUID barberId);

    List<BarberWorkHours> findByBarberIdAndIsActive(UUID barberId, boolean isActive);

    Optional<BarberWorkHours> findByBarberIdAndDayOfWeek(UUID barberId, DayOfWeek dayOfWeek);

    List<BarberWorkHours> findByBarbershopId(UUID barbershopId);

    void deleteByBarberId(UUID barberId);
}
