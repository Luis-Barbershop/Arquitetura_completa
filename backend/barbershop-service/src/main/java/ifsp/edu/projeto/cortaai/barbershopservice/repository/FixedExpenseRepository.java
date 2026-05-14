package ifsp.edu.projeto.cortaai.barbershopservice.repository;

import ifsp.edu.projeto.cortaai.barbershopservice.model.FixedExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FixedExpenseRepository extends JpaRepository<FixedExpense, UUID> {

    @Query("""
        select e
        from FixedExpense e
        where e.barbershopId = :barbershopId
          and (
            (coalesce(e.recurringMonthly, false) = false and e.month = :month and e.year = :year)
            or
            (coalesce(e.recurringMonthly, false) = true and (e.year < :year or (e.year = :year and e.month <= :month)))
          )
        order by e.category asc, e.customName asc
    """)
    List<FixedExpense> findActiveForMonth(
        @Param("barbershopId") UUID barbershopId,
        @Param("month") Integer month,
        @Param("year") Integer year
    );

    @Query("""
        select e
        from FixedExpense e
        where e.barbershopId = :barbershopId
          and (
            (coalesce(e.recurringMonthly, false) = false and e.year = :year)
            or
            (coalesce(e.recurringMonthly, false) = true and e.year <= :year)
          )
        order by e.month asc, e.category asc, e.customName asc
    """)
    List<FixedExpense> findActiveForYear(
        @Param("barbershopId") UUID barbershopId,
        @Param("year") Integer year
    );
}
