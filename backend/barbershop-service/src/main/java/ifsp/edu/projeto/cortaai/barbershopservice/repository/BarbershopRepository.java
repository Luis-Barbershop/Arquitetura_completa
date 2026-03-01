package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BarbershopRepository extends JpaRepository<Barbershop, UUID> {
    Optional<Barbershop> findByCnpj(String cnpj);
    Optional<Barbershop> findByOwnerId(UUID ownerId);
    boolean existsByCnpj(String cnpj);
}

