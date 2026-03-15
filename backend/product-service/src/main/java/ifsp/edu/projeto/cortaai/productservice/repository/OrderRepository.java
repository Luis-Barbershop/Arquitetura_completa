package ifsp.edu.projeto.cortaai.productservice.repository;

import ifsp.edu.projeto.cortaai.productservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") UUID customerId);

    @Query("SELECT o FROM Order o WHERE o.barbershopId = :barbershopId ORDER BY o.createdAt DESC")
    List<Order> findByBarbershopIdOrderByCreatedAtDesc(@Param("barbershopId") UUID barbershopId);
}