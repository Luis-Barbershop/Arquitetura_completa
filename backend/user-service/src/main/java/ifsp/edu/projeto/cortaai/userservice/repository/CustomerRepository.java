package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByFirebaseUid(String firebaseUid);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByFirebaseUid(String firebaseUid);
    boolean existsByDocumentCPF(String documentCPF);
    boolean existsByDocumentCPFIgnoreCase(String documentCPF);
    boolean existsByTell(String tell);
    boolean existsByTellIgnoreCase(String tell);
}