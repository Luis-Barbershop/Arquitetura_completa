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

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
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
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(nullable = false, length = 70)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String tell;

    @Column(nullable = false, unique = true, length = 70)
    private String email;

    @Column(name = "document_cpf", nullable = false, unique = true, length = 11)
    private String documentCPF;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_owner")
    private boolean isOwner = false;

    @Column(length = 20)
    private String role = "ROLE_BARBER";

    // DESACOPLAMENTO: Apenas o ID da barbearia (que vive em outro banco)
    // Mudado para UUID pois o banco da barbearia usará UUID
    @Column(name = "barbershop_id", length = 36)
    private UUID barbershopId;

    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_url_public_id")
    private String imageUrlPublicId;

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;

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