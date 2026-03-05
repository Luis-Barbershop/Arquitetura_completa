package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopHighlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BarbershopHighlightRepository extends JpaRepository<BarbershopHighlight, UUID> {

    @Query("SELECT bh FROM BarbershopHighlight bh WHERE bh.barbershop.id = :barbershopId")
    List<BarbershopHighlight> findByBarbershopId(@Param("barbershopId") UUID barbershopId);
    
};