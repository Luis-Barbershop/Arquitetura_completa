package ifsp.edu.projeto.cortaai.repository;

import ifsp.edu.projeto.cortaai.model.Customer;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

    boolean existsByPreviousAppointmentIgnoreCase(String previousAppointment);

}
