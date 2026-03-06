package ifsp.edu.projeto.cortaai.paymentservice.repository;

import ifsp.edu.projeto.cortaai.paymentservice.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    @Query("SELECT w FROM WebhookLog w WHERE w.mpResourceId = :mpResourceId")
    Optional<WebhookLog> findByMpResourceId(@Param("mpResourceId") String mpResourceId);

    @Query("SELECT CASE WHEN COUNT(w) > 0 THEN true ELSE false END FROM WebhookLog w WHERE w.mpResourceId = :mpResourceId AND w.processed = true")
    boolean existsByMpResourceIdAndProcessedTrue(@Param("mpResourceId") String mpResourceId);
}