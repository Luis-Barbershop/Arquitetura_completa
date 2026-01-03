package ifsp.edu.projeto.cortaai.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalTime;

@Entity
@Table(name = "barbers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Barber {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 70)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String tell;

    @Column(nullable = false, unique = true, length = 70)
    private String email;

    @Column(name = "document_cpf", nullable = false, unique = true, length = 11)
    private String documentCPF;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "is_owner", nullable = false)
    private boolean isOwner = false;

    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    // Relacionamento: N Barbeiros pertencem a 1 Barbearia
    // É NULLABLE para permitir barbeiros "livres"
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbershop_id")
    private Barbershop barbershop;

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    // Relacionamento: 1 Barbeiro tem N Agendamentos
    @OneToMany(mappedBy = "barber")
    private Set<Appointments> appointments;

    // Relacionamento: N Barbeiros realizam N Serviços
    @ManyToMany
    @JoinTable(
            name = "barber_activities",
            joinColumns = @JoinColumn(name = "barber_id"),
            inverseJoinColumns = @JoinColumn(name = "activity_id")
    )
    private Set<Activity> activities;

    // Relacionamento: 1 Barbeiro tem N Pedidos para Entrar
    @OneToMany(mappedBy = "barber")
    private Set<BarbershopJoinRequest> joinRequests;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "image_url_public_id", length = 255) // NOVA COLUNA
    private String imageUrlPublicId;
}