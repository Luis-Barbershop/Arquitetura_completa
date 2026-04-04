package ifsp.edu.projeto.cortaai.productservice.repository;

import ifsp.edu.projeto.cortaai.productservice.model.StockMovement;
import ifsp.edu.projeto.cortaai.productservice.model.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    @Query("SELECT s FROM StockMovement s WHERE s.productId = :productId ORDER BY s.createdAt DESC")
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(@Param("productId") UUID productId);

    @Query("SELECT s FROM StockMovement s WHERE s.productId IN :productIds AND s.type = :type " +
            "AND s.createdAt BETWEEN :from AND :to")
    List<StockMovement> findByProductIdsAndTypeAndCreatedAtBetween(
            @Param("productIds") List<UUID> productIds,
            @Param("type") MovementType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}