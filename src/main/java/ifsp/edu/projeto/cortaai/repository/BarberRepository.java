package ifsp.edu.projeto.cortaai.repository;

import ifsp.edu.projeto.cortaai.model.Barber;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BarberRepository extends JpaRepository<Barber, UUID> {

    boolean existsByTellIgnoreCase(String tell);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocumentCPFIgnoreCase(String documentCPF);

}
