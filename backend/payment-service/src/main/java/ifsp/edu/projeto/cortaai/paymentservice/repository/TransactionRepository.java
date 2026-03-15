package ifsp.edu.projeto.cortaai.paymentservice.repository;

import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT t FROM Transaction t WHERE t.customerId = :customerId ORDER BY t.createdAt DESC")
    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);

    @Query("SELECT t FROM Transaction t WHERE t.mpPreferenceId = :mpPreferenceId")
    Optional<Transaction> findByMpPreferenceId(@Param("mpPreferenceId") String mpPreferenceId);

    @Query("SELECT t FROM Transaction t WHERE t.appointmentId = :appointmentId")
    Optional<Transaction> findByAppointmentId(@Param("appointmentId") UUID appointmentId);
}