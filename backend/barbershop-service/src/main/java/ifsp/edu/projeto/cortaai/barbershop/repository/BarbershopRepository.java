package ifsp.edu.projeto.cortaai.barbershop.repository;

import ifsp.edu.projeto.cortaai.barbershop.model.Barbershop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarbershopRepository extends JpaRepository<Barbershop, UUID> {
    
    Optional<Barbershop> findByCnpj(String cnpj);
    
    Optional<Barbershop> findByOwnerId(UUID ownerId);
    
    boolean existsByCnpj(String cnpj);
}
