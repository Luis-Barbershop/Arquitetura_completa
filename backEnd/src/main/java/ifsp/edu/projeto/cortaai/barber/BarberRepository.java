package ifsp.edu.projeto.cortaai.barber;

import java.util.UUID;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface BarberRepository extends MongoRepository<Barber, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

}
