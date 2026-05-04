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

    @Query(value = "SELECT * FROM barbershops WHERE cnpj IS NOT NULL AND cnpj <> '' AND cnpj NOT LIKE 'enc:v1:%'", nativeQuery = true)
    List<Barbershop> findWithLegacyPlainCnpj();
}
