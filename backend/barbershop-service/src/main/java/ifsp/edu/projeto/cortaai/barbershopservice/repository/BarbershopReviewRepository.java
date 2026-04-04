package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BarbershopReviewRepository extends JpaRepository<BarbershopReview, UUID> {

	boolean existsByBarbershop_IdAndCustomerId(UUID barbershopId, UUID customerId);
}


