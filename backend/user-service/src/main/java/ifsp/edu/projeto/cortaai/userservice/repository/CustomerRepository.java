package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    // O JPA entende que documentCPF (camelCase) mapeia para document_cpf (snake_case)
    // graças à anotação @Column na entidade.
    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

    // Método findByEmail (sem "IgnoreCase") é usado pelo login.
    Optional<Customer> findByEmail(String email);
}