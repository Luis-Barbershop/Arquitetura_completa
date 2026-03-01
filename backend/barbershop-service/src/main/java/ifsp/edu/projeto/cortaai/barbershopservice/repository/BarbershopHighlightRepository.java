package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopHighlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BarbershopHighlightRepository extends JpaRepository<BarbershopHighlight, UUID> {
    List<BarbershopHighlight> findByBarbershopId(UUID barbershopId);
}

