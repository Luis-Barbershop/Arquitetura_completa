package ifsp.edu.projeto.cortaai.userservice.repository.analytics;

import ifsp.edu.projeto.cortaai.userservice.model.analytics.VCustomerAcquisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VCustomerAcquisitionRepository extends JpaRepository<VCustomerAcquisition, String> {
}