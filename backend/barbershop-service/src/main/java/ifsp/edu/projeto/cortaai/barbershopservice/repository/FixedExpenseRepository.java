package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.FixedExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, UUID> {

    List<FixedExpense> findByBarbershopIdAndMonthAndYearOrderByCategoryAsc(
        UUID barbershopId, Integer month, Integer year
    );

    List<FixedExpense> findByBarbershopIdAndYearOrderByMonthAscCategoryAsc(
        UUID barbershopId, Integer year
    );
}
