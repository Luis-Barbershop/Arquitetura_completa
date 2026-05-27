package ifsp.edu.projeto.cortaai.paymentservice.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Immutable
@Table(name = "v_daily_financial_summary")
public class VDailyFinancialSummary {
    @Id
    private LocalDate referenceDate;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
    private Integer transactionCount;
}