package ifsp.edu.projeto.cortaai.userservice.repository;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarberRepository extends JpaRepository<Barber, UUID> {
    Optional<Barber> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByDocumentCPF(String documentCPF);
    boolean existsByDocumentCPFIgnoreCase(String documentCPF);
    boolean existsByTellIgnoreCase(String tell);
    List<Barber> findByBarbershopId(UUID barbershopId);
}