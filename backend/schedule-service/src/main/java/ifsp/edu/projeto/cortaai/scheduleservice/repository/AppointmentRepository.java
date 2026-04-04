package ifsp.edu.projeto.cortaai.scheduleservice.repository;

import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.barberId = :barberId " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "AND a.startTime < :endTime AND a.endTime > :startTime")
    boolean hasConflict(@Param("barberId") UUID barberId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

    List<Appointment> findByCustomerIdOrderByStartTimeDesc(UUID customerId);

    List<Appointment> findByBarberIdOrderByStartTimeDesc(UUID barberId);

    List<Appointment> findByBarberIdAndStartTimeBetween(UUID barberId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByBarbershopIdAndStartTimeBetween(UUID barbershopId, LocalDateTime start, LocalDateTime end);
}

