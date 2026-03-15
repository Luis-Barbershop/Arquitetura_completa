package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BarbershopRepository extends JpaRepository<Barbershop, UUID> {

    @Query("SELECT b FROM Barbershop b WHERE b.cnpj = :cnpj")
    Optional<Barbershop> findByCnpj(@Param("cnpj") String cnpj);

    @Query("SELECT b FROM Barbershop b WHERE b.ownerId = :ownerId")
    Optional<Barbershop> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Barbershop b WHERE b.cnpj = :cnpj")
    boolean existsByCnpj(@Param("cnpj") String cnpj);
}