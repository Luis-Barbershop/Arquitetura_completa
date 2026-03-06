package ifsp.edu.projeto.cortaai.productservice.repository;

import ifsp.edu.projeto.cortaai.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("SELECT p FROM Product p WHERE p.barbershopId = :barbershopId AND p.active = true")
    List<Product> findByBarbershopIdAndActiveTrue(@Param("barbershopId") UUID barbershopId);

    @Query("SELECT p FROM Product p WHERE p.barbershopId = :barbershopId")
    List<Product> findByBarbershopId(@Param("barbershopId") UUID barbershopId);
}