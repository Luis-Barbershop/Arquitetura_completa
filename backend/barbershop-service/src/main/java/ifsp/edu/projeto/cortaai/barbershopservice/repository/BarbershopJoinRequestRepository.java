package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopJoinRequest;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarbershopJoinRequestRepository extends JpaRepository<BarbershopJoinRequest, UUID> {
    List<BarbershopJoinRequest> findByBarbershopIdAndStatus(UUID barbershopId, JoinRequestStatus status);
    Optional<BarbershopJoinRequest> findByBarberIdAndBarbershopId(UUID barberId, UUID barbershopId);
    List<BarbershopJoinRequest> findByBarberId(UUID barberId);
}

