package ifsp.edu.projeto.cortaai.repository;

import java.util.UUID;

import ifsp.edu.projeto.cortaai.model.Barber;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface BarberRepository extends MongoRepository<Barber, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

}
