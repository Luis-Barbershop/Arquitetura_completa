package ifsp.edu.projeto.cortaai.barbershopservice.model;

import ifsp.edu.projeto.cortaai.barbershopservice.model.enums.JoinRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
    @Column(nullable = false, updatable = false, length = 36)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // Desacoplado: apenas o UUID do barbeiro (vive no user-service)
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