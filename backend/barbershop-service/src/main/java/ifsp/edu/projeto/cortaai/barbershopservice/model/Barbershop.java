package ifsp.edu.projeto.cortaai.barbershopservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.Formula;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.sql.Types;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "barbershops")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Barbershop {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    // ID do dono (Barber) — armazenado como UUID string, vive no user-service
    @JdbcTypeCode(Types.VARCHAR)
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

    @Formula("(select avg(br.rating) from barbershop_reviews br where br.barbershop_id = id)")
    private Double averageRating;

    @Formula("(select count(*) from barbershop_reviews br where br.barbershop_id = id)")
    private Long reviewsCount;

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // Relacionamento: 1 Barbearia tem N Serviços
    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Activity> activities;

    // Relacionamento: 1 Barbearia tem N Pedidos para Entrar
    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BarbershopJoinRequest> joinRequests;

    // Relacionamento: 1 Barbearia tem N Destaques
    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BarbershopHighlight> highlights;

    @OneToMany(mappedBy = "barbershop", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BarbershopReview> reviews;
}