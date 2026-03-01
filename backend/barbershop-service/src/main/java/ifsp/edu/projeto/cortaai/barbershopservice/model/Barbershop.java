package ifsp.edu.projeto.cortaai.barbershopservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "barbershops")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Barbershop {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // ID do dono (Barber) — armazenado como UUID, vive no user-service
    @Column(name = "owner_id", nullable = false, length = 36)
    private UUID ownerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(length = 255)
    private String address;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "logo_url_public_id", length = 255)
    private String logoUrlPublicId;

    @Column(name = "banner_url", length = 255)
    private String bannerUrl;

    @Column(name = "banner_url_public_id", length = 255)
    private String bannerUrlPublicId;

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

    // Relacionamento: 1 Barbearia tem N Serviços
    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Activity> activities;

    // Relacionamento: 1 Barbearia tem N Pedidos para Entrar
    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BarbershopJoinRequest> joinRequests;

    // Relacionamento: 1 Barbearia tem N Destaques
    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BarbershopHighlight> highlights;
}

