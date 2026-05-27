package ifsp.edu.projeto.cortaai.userservice.repository.analytics;

import ifsp.edu.projeto.cortaai.userservice.model.analytics.VCustomerRetention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VCustomerRetentionRepository extends JpaRepository<VCustomerRetention, String> {
}