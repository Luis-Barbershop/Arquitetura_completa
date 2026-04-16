package ifsp.edu.projeto.cortaai.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "barbers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Barber implements UserDetails {

    @Id
    @GeneratedValue
    @UuidGenerator
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(nullable = false, length = 70)
    private String name;

    // CORREÇÃO: O telefone agora é opcional (removido o nullable = false)
    @Column(unique = true, length = 15)
    private String tell;

    @Column(nullable = false, unique = true, length = 70)
    private String email;

    @Column(name = "document_cpf", unique = true, length = 14)
    private String documentCPF;

    @Column
    private String password;

    // ======== Firebase Auth ========
    /** UID único emitido pelo Firebase Authentication (Google, Facebook, Apple, GitHub…) */
    @Column(name = "firebase_uid", unique = true, length = 128)
    private String firebaseUid;

    /** Provedor de autenticação: EMAIL, GOOGLE, FACEBOOK, APPLE, GITHUB, TWITTER */
    @Builder.Default
    @Column(name = "auth_provider", length = 30)
    private String authProvider = "EMAIL";

    @Builder.Default
    @Column(name = "is_owner")
    private boolean isOwner = false;

    /**
     * Indica se o dono do estabelecimento também atua como barbeiro
     * (aparece na lista de profissionais disponíveis para agendamento).
     * Padrão: true — o owner aparece como barbeiro até que configure o contrário.
     */
    @Builder.Default
    @Column(name = "act_as_barber", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean actAsBarber = true;

    @Builder.Default
    @Column(length = 20)
    private String role = "ROLE_BARBER";

    // DESACOPLAMENTO: Apenas o ID da barbearia (que vive noutro banco)
    @Column(name = "barbershop_id", length = 36)
    @JdbcTypeCode(Types.VARCHAR)
    private UUID barbershopId;

    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_url_public_id")
    private String imageUrlPublicId;

    // ======== Mercado Pago OAuth ========
    /**
     * Access token OAuth do barbeiro no Mercado Pago.
     * Usado para criar pagamentos em nome dele no split.
     */
    @Column(name = "mp_access_token", columnDefinition = "TEXT")
    private String mpAccessToken;

    /**
     * Refresh token para renovar o mpAccessToken quando expirar.
     */
    @Column(name = "mp_refresh_token", columnDefinition = "TEXT")
    private String mpRefreshToken;

    /**
     * ID numérico do usuário no Mercado Pago (collector_id).
     * Referenciado como marketplace_owner_id no split.
     */
    @Column(name = "mp_user_id", length = 60)
    private String mpUserId;

    /**
     * Public key do MP do barbeiro — usada no front-end para tokenizar cartão.
     */
    @Column(name = "mp_public_key", length = 100)
    private String mpPublicKey;

    /** UUIDs das atividades (serviços) que este barbeiro sabe executar. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "barber_assigned_activities",
            joinColumns = @JoinColumn(name = "barber_id")
    )
    @Column(name = "activity_id", length = 36)
    @Builder.Default
    private Set<UUID> assignedActivityIds = new HashSet<>();

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Métodos do UserDetails para o Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isOwner) {
            return List.of(new SimpleGrantedAuthority("ROLE_BARBER"), new SimpleGrantedAuthority("ROLE_OWNER"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_BARBER"));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}