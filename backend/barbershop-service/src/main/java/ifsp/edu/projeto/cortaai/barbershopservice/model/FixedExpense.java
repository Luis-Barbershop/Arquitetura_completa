package ifsp.edu.projeto.cortaai.barbershopservice.model;

import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.FixedExpenseCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fixed_expenses", indexes = {
    @Index(name = "idx_fe_barbershop_month_year", columnList = "barbershop_id,month,year")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class FixedExpense {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "barbershop_id", nullable = false, length = 36)
    private UUID barbershopId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FixedExpenseCategory category;

    @Column(name = "custom_name", length = 80)
    private String customName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer month;

    @Column(nullable = false)
    private Integer year;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
