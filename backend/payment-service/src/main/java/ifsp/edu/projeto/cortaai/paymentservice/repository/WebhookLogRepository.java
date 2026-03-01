package ifsp.edu.projeto.cortaai.paymentservice.repository;

import ifsp.edu.projeto.cortaai.paymentservice.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    Optional<WebhookLog> findByMpResourceId(String mpResourceId);

    boolean existsByMpResourceIdAndProcessedTrue(String mpResourceId);
}
