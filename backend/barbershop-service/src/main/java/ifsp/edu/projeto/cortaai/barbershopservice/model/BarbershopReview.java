package ifsp.edu.projeto.cortaai.barbershopservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "barbershop_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_review_customer_shop", columnNames = {"customer_id", "barbershop_id"})
        }
)
@Getter
@Setter
public class BarbershopReview {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "customer_id", nullable = false, length = 36)
    private UUID customerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barbershop_id", nullable = false)
    private Barbershop barbershop;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}

