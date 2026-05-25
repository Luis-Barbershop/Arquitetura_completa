package ifsp.edu.projeto.cortaai.scheduleservice.repository;

import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.projection.AgendaThermometerProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

                @Lock(LockModeType.PESSIMISTIC_WRITE)
                @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
                    "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
                    "AND a.startTime < :endTime AND a.endTime > :startTime")
                List<Appointment> findConflictsForUpdate(@Param("barberId") UUID barberId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

                @Lock(LockModeType.PESSIMISTIC_WRITE)
                @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
                        "AND a.id <> :appointmentId " +
                        "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
                        "AND a.startTime < :endTime AND a.endTime > :startTime")
                List<Appointment> findConflictsForUpdateExcludingAppointment(@Param("barberId") UUID barberId,
                                                                             @Param("appointmentId") UUID appointmentId,
                                                                             @Param("startTime") LocalDateTime startTime,
                                                                             @Param("endTime") LocalDateTime endTime);

                    @Lock(LockModeType.PESSIMISTIC_WRITE)
                    @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
                            "AND a.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED', 'CONCLUDED') " +
                            "AND a.startTime < :endTime AND a.endTime > :startTime " +
                            "ORDER BY a.startTime ASC")
                    List<Appointment> findAppointmentsToCancelForBarberBlock(@Param("barberId") UUID barberId,
                                                                             @Param("startTime") LocalDateTime startTime,
                                                                             @Param("endTime") LocalDateTime endTime);

    List<Appointment> findByCustomerIdOrderByStartTimeDesc(UUID customerId);

    List<Appointment> findByBarberIdOrderByStartTimeDesc(UUID barberId);

    List<Appointment> findByBarberIdAndStartTimeBetween(UUID barberId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findByBarbershopIdAndStartTimeBetween(UUID barbershopId, LocalDateTime start, LocalDateTime end);

    @Query(value = """
            SELECT
                DATE(a.start_time) AS agendaDate,
                a.barbershop_id AS barbershopId,
                COUNT(a.id) AS totalAppointments,
                COALESCE(SUM(CASE WHEN a.status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS') THEN 1 ELSE 0 END), 0) AS activeAppointments,
                COALESCE(SUM(CASE WHEN a.status = 'WALK_IN' THEN 1 ELSE 0 END), 0) AS walkinAppointments,
                COALESCE(SUM(CASE WHEN a.status IN ('PAYMENT_PENDING', 'EXPIRED') THEN 1 ELSE 0 END), 0) AS pendingAppointments,
                COALESCE(SUM(CASE WHEN a.status IN ('COMPLETED', 'CONCLUDED') THEN 1 ELSE 0 END), 0) AS completedAppointments,
                COALESCE(SUM(CASE WHEN a.status IN ('CANCELLED', 'NO_SHOW') THEN 1 ELSE 0 END), 0) AS lostAppointments
            FROM appointments a
            WHERE a.barbershop_id = :barbershopId
            GROUP BY DATE(a.start_time), a.barbershop_id
            ORDER BY DATE(a.start_time) ASC
            """, nativeQuery = true)
    List<AgendaThermometerProjection> findAgendaThermometerByBarbershopId(@Param("barbershopId") String barbershopId);

    @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
            "AND a.startTime >= :from " +
            "AND a.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED', 'CONCLUDED') " +
            "ORDER BY a.startTime ASC")
    List<Appointment> findFutureActiveByBarberId(
            @Param("barberId") UUID barberId,
            @Param("from") LocalDateTime from);

    @Query(value = "SELECT * FROM appointments WHERE customer_name IS NOT NULL AND customer_name <> '' AND customer_name NOT LIKE 'enc:v1:%'", nativeQuery = true)
    List<Appointment> findWithLegacyPlainCustomerName();

    // ── Queries para o chat IA (gustave) ───────────────────────────────────

    @Query("SELECT a FROM Appointment a WHERE a.barbershopId = :barbershopId " +
           "AND a.startTime >= :from " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findUpcomingByBarbershop(
            @Param("barbershopId") UUID barbershopId,
            @Param("from") LocalDateTime from);

    @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
           "AND a.startTime >= :from " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findUpcomingByBarberId(
            @Param("barberId") UUID barberId,
            @Param("from") LocalDateTime from);

    @Query("SELECT a FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.startTime >= :from " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW') " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findUpcomingByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("from") LocalDateTime from);

    @Query("SELECT a FROM Appointment a WHERE a.barbershopId = :barbershopId " +
           "AND a.status IN ('COMPLETED', 'CONCLUDED') " +
           "AND a.startTime BETWEEN :from AND :to " +
           "ORDER BY a.startTime DESC")
    List<Appointment> findCompletedByBarbershop(
            @Param("barbershopId") UUID barbershopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE a.barberId = :barberId " +
           "AND a.status IN ('COMPLETED', 'CONCLUDED') " +
           "AND a.startTime BETWEEN :from AND :to " +
           "ORDER BY a.startTime DESC")
    List<Appointment> findCompletedByBarberId(
            @Param("barberId") UUID barberId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.status IN ('COMPLETED', 'CONCLUDED') " +
           "AND a.startTime BETWEEN :from AND :to " +
           "ORDER BY a.startTime DESC")
    List<Appointment> findCompletedByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE a.customerId = :customerId " +
           "AND a.status IN ('CANCELLED', 'NO_SHOW') " +
           "AND a.startTime BETWEEN :from AND :to " +
           "ORDER BY a.startTime DESC")
    List<Appointment> findCancelledByCustomerId(
            @Param("customerId") UUID customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE a.barbershopId = :barbershopId " +
           "AND a.status IN ('CANCELLED', 'NO_SHOW') " +
           "AND a.startTime BETWEEN :from AND :to " +
           "ORDER BY a.startTime DESC")
    List<Appointment> findCancelledByBarbershop(
            @Param("barbershopId") UUID barbershopId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE " +
           "a.startTime BETWEEN :from AND :to " +
           "AND a.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED', 'CONCLUDED', 'EXPIRED')")
    List<Appointment> findActiveInTimeWindow(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE " +
           "a.startTime BETWEEN :from AND :to " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS') " +
           "ORDER BY a.startTime ASC")
    List<Appointment> findAppointmentsForReminderWindow(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.status = 'PAYMENT_PENDING' " +
            "AND a.dateCreated <= :cutoff " +
            "ORDER BY a.dateCreated ASC")
    List<Appointment> findExpiredPaymentPendingAppointments(
            @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a FROM Appointment a WHERE " +
            "a.status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'WALK_IN') " +
            "AND a.endTime <= :cutoff " +
            "ORDER BY a.endTime ASC")
    List<Appointment> findAppointmentsReadyForAutoCompletion(
            @Param("cutoff") LocalDateTime cutoff);
}
