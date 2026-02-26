package ifsp.edu.projeto.cortaai.schedule.repository;

import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByBarbershopId(UUID barbershopId);

    List<Appointment> findByBarberId(UUID barberId);

    List<Appointment> findByCustomerId(UUID customerId);

    List<Appointment> findByBarberIdAndStatus(UUID barberId, AppointmentStatus status);

    List<Appointment> findByBarbershopIdAndStatus(UUID barbershopId, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
           "AND a.startTime >= :start AND a.endTime <= :end " +
           "AND a.status NOT IN ('CANCELLED')")
    List<Appointment> findByBarberIdAndTimeRange(
            @Param("barberId") UUID barberId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

    @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
           "AND ((a.startTime < :endTime AND a.endTime > :startTime)) " +
           "AND a.status NOT IN ('CANCELLED')")
    List<Appointment> findConflictingAppointments(
            @Param("barberId") UUID barberId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    @Query("SELECT a FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.startTime > :now AND a.status = 'SCHEDULED'")
    List<Appointment> findUpcomingAppointments(
            @Param("customerId") UUID customerId,
            @Param("now") OffsetDateTime now);
}
