package ifsp.edu.projeto.cortaai.barbershopservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "barber_commission_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_barber_activity",
                columnNames = {"barbershop_id", "barber_id", "activity_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BarberCommissionRule {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "barbershop_id", nullable = false, length = 36)
    private UUID barbershopId;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "barber_id", nullable = false, length = 36)
    private UUID barberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
