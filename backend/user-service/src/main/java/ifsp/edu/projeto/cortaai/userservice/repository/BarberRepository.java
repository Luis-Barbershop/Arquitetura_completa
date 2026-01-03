package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Note: JpaRepository<Barber, UUID>
public interface BarberRepository extends JpaRepository<Barber, UUID> {
    Optional<Barber> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByDocumentCPF(String documentCPF);
    boolean existsByTell(String tell);

    // Método para buscar barbeiros de uma barbearia específica
    List<Barber> findByBarbershopId(Long barbershopId);
}