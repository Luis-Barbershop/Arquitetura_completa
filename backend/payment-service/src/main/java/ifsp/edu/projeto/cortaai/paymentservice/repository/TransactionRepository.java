package ifsp.edu.projeto.cortaai.paymentservice.repository;

import ifsp.edu.projeto.cortaai.paymentservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<Transaction> findByMpPreferenceId(String mpPreferenceId);

    Optional<Transaction> findByAppointmentId(UUID appointmentId);
}
