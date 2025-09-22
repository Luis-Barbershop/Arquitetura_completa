package ifsp.edu.projeto.cortaai.repository;

import java.util.UUID;

import ifsp.edu.projeto.cortaai.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CustomerRepository extends MongoRepository<Customer, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

    boolean existsByPreviousAppointmentIgnoreCase(String previousAppointment);

}
