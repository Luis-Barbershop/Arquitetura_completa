package ifsp.edu.projeto.cortaai.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dashboard_kpi_daily",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dashboard_kpi_daily_shop_date", columnNames = {"barbershop_id", "reference_date"})
        },
        indexes = {
                @Index(name = "idx_dashboard_kpi_daily_shop_date", columnList = "barbershop_id,reference_date"),
                @Index(name = "idx_dashboard_kpi_daily_reference_date", columnList = "reference_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKpiDaily {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "barbershop_id", nullable = false)
    private UUID barbershopId;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "approved_revenue", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal approvedRevenue = BigDecimal.ZERO;

    @Column(name = "approved_transactions_count", nullable = false)
    @Builder.Default
    private Integer approvedTransactionsCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touchUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
