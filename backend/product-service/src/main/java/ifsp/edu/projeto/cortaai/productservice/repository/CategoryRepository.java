package ifsp.edu.projeto.cortaai.productservice.repository;

import ifsp.edu.projeto.cortaai.productservice.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByBarbershopIdOrderByNameAsc(UUID barbershopId);

    Optional<Category> findByIdAndBarbershopId(UUID id, UUID barbershopId);

    boolean existsByNameIgnoreCaseAndBarbershopId(String name, UUID barbershopId);
}
