package ifsp.edu.projeto.cortaai.barbershopservice.model;

import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "barbershop_join_requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"barber_id", "barbershop_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BarbershopJoinRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    // Desacoplado: apenas o UUID do barbeiro (vive no user-service)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "barber_id", nullable = false, length = 36)
    private UUID barberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbershop_id", nullable = false)
    private Barbershop barbershop;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private JoinRequestStatus status;

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;
}