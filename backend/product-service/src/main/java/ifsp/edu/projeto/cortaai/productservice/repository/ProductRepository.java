package ifsp.edu.projeto.cortaai.productservice.repository;

import ifsp.edu.projeto.cortaai.productservice.model.Product;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT p FROM Product p WHERE p.barbershopId = :barbershopId AND p.active = true " +
            "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:categoryId IS NULL OR p.dynamicCategory.id = :categoryId) " +
            "AND (:lowStock IS NULL OR (:lowStock = true AND p.stockQuantity <= p.minStockQuantity) " +
            "OR (:lowStock = false AND p.stockQuantity > p.minStockQuantity))")
    Page<Product> findInventoryPageByFilters(
            @Param("barbershopId") UUID barbershopId,
            @Param("search") String search,
            @Param("category") ProductCategory category,
            @Param("categoryId") UUID categoryId,
            @Param("lowStock") Boolean lowStock,
            Pageable pageable);

    boolean existsByDynamicCategoryIdAndActiveTrue(UUID categoryId);
}
