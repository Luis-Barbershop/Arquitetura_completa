package ifsp.edu.projeto.cortaai.barbershop.repository;

import ifsp.edu.projeto.cortaai.barbershop.model.BarbershopHighlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BarbershopHighlightRepository extends JpaRepository<BarbershopHighlight, UUID> {
    
    List<BarbershopHighlight> findByBarbershopId(UUID barbershopId);
}
