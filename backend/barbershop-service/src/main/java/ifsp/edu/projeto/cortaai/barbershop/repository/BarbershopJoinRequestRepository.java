package ifsp.edu.projeto.cortaai.barbershop.repository;

import ifsp.edu.projeto.cortaai.barbershop.model.BarbershopJoinRequest;
import ifsp.edu.projeto.cortaai.barbershop.model.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BarbershopJoinRequestRepository extends JpaRepository<BarbershopJoinRequest, Long> {
    
    List<BarbershopJoinRequest> findByBarbershopIdAndStatus(UUID barbershopId, JoinRequestStatus status);
    
    Optional<BarbershopJoinRequest> findByBarberIdAndStatus(UUID barberId, JoinRequestStatus status);
    
    boolean existsByBarberIdAndStatus(UUID barberId, JoinRequestStatus status);
    
    List<BarbershopJoinRequest> findByBarberId(UUID barberId);
}
