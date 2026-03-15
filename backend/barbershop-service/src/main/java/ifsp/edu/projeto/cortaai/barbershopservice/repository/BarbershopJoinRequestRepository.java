package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopJoinRequest;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BarbershopJoinRequestRepository extends JpaRepository<BarbershopJoinRequest, UUID> {

    @Query("SELECT r FROM BarbershopJoinRequest r WHERE r.barbershop.id = :barbershopId AND r.status = :status")
    List<BarbershopJoinRequest> findByBarbershopIdAndStatus(
            @Param("barbershopId") UUID barbershopId, 
            @Param("status") JoinRequestStatus status
    );

    // CORRIGIDO: de r.barber.id para r.barberId
    @Query("SELECT r FROM BarbershopJoinRequest r WHERE r.barberId = :barberId AND r.barbershop.id = :barbershopId")
    Optional<BarbershopJoinRequest> findByBarberIdAndBarbershopId(
            @Param("barberId") UUID barberId, 
            @Param("barbershopId") UUID barbershopId
    );

    // CORRIGIDO: de r.barber.id para r.barberId
    @Query("SELECT r FROM BarbershopJoinRequest r WHERE r.barberId = :barberId")
    List<BarbershopJoinRequest> findByBarberId(
            @Param("barberId") UUID barberId
    );
}