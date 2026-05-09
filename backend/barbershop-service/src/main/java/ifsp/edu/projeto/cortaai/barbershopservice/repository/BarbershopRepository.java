package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface BarbershopRepository extends JpaRepository<Barbershop, UUID> {

    default Optional<Barbershop> findByCnpj(String cnpj) {
        return findByCnpjEncrypted(cnpj).or(() -> findByCnpjRaw(cnpj));
    }

    @Query("SELECT b FROM Barbershop b WHERE b.cnpj = :cnpj")
    Optional<Barbershop> findByCnpjEncrypted(@Param("cnpj") String cnpj);

    @Query(value = "SELECT * FROM barbershops WHERE cnpj = :cnpj LIMIT 1", nativeQuery = true)
    Optional<Barbershop> findByCnpjRaw(@Param("cnpj") String cnpj);

    @Query("SELECT b FROM Barbershop b WHERE b.ownerId = :ownerId")
    Optional<Barbershop> findByOwnerId(@Param("ownerId") UUID ownerId);

    default boolean existsByCnpj(String cnpj) {
        return existsByCnpjEncrypted(cnpj) || countByCnpjRaw(cnpj) > 0;
    }

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barbershop b WHERE b.cnpj = :cnpj")
    boolean existsByCnpjEncrypted(@Param("cnpj") String cnpj);

    @Query(value = "SELECT COUNT(*) FROM barbershops WHERE cnpj = :cnpj", nativeQuery = true)
    long countByCnpjRaw(@Param("cnpj") String cnpj);

    @Query("SELECT b FROM Barbershop b WHERE b.cnpj IS NOT NULL AND b.cnpj <> '' AND b.cnpj NOT LIKE 'enc:v1:%'")
    List<Barbershop> findWithLegacyPlainCnpj();

    @Query(value = """
            SELECT *, (6371 * acos(
                cos(radians(:lat)) * cos(radians(latitude)) *
                cos(radians(longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(latitude))
            )) AS distance_km
            FROM barbershops
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
            HAVING distance_km <= :radiusKm
            ORDER BY distance_km
            """, nativeQuery = true)
    List<Barbershop> findByProximity(@Param("lat") Double lat,
                                     @Param("lng") Double lng,
                                     @Param("radiusKm") Double radiusKm);
}
