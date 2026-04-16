package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.BarbershopJoinRequest;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestType;
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

    @Query("SELECT r FROM BarbershopJoinRequest r WHERE r.barbershop.id = :barbershopId AND r.status = :status AND r.requestType = :type")
    List<BarbershopJoinRequest> findByBarbershopIdAndStatusAndRequestType(
            @Param("barbershopId") UUID barbershopId,
            @Param("status") JoinRequestStatus status,
            @Param("type") JoinRequestType type
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

    /** Busca convites (INVITE) pendentes para um barbeiro específico. */
    @Query("SELECT r FROM BarbershopJoinRequest r JOIN FETCH r.barbershop WHERE r.barberId = :barberId AND r.status = :status AND r.requestType = :type")
    List<BarbershopJoinRequest> findByBarberIdAndStatusAndRequestType(
            @Param("barberId") UUID barberId,
            @Param("status") JoinRequestStatus status,
            @Param("type") JoinRequestType type
    );
}