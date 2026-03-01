package ifsp.edu.projeto.cortaai.scheduleservice.repository;

import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BarberBlockRepository extends JpaRepository<BarberBlock, UUID> {

    List<BarberBlock> findByBarberIdAndStartTimeBetween(UUID barberId, LocalDateTime start, LocalDateTime end);

    boolean existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
            UUID barberId, LocalDateTime endTime, LocalDateTime startTime);
}

