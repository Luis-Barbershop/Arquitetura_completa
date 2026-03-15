package ifsp.edu.projeto.cortaai.scheduleservice.repository;

import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BarberBlockRepository extends JpaRepository<BarberBlock, UUID> {

    @Query("SELECT b FROM BarberBlock b WHERE b.barberId = :barberId AND b.startTime BETWEEN :start AND :end")
    List<BarberBlock> findByBarberIdAndStartTimeBetween(
            @Param("barberId") UUID barberId, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BarberBlock b WHERE b.barberId = :barberId AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
            @Param("barberId") UUID barberId, 
            @Param("endTime") LocalDateTime endTime, 
            @Param("startTime") LocalDateTime startTime);
}