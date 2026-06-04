package ifsp.edu.projeto.cortaai.userservice.model;

import ifsp.edu.projeto.cortaai.userservice.security.crypto.SensitiveStringConverter;
import ifsp.edu.projeto.cortaai.userservice.security.crypto.SensitiveLocalDateConverter;
import ifsp.edu.projeto.cortaai.userservice.security.crypto.PrivacyHash;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.sql.Types;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Customer {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    @GeneratedValue
    @JdbcTypeCode(Types.VARCHAR)
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 70)
    private String name;

    // CORREÇÃO: O telefone agora é opcional (removido o nullable = false)
    @Convert(converter = SensitiveStringConverter.class)
    @Column(unique = true, length = 128)
    private String tell;

    @Convert(converter = SensitiveStringConverter.class)
    @Column(nullable = false, unique = true, length = 256)
    private String email;

    @Column(name = "email_hash", unique = true, length = 64)
    private String emailHash;

    @Convert(converter = SensitiveStringConverter.class)
    @Column(name = "document_cpf", unique = true, length = 128)
    private String documentCPF;

    @Column(length = 255)
    private String password;

    // ======== Firebase Auth ========
    /** UID único emitido pelo Firebase Authentication (Google, Facebook, Apple, GitHub…) */
    @Column(name = "firebase_uid", unique = true, length = 128)
    private String firebaseUid;

    /** Provedor de autenticação: EMAIL, GOOGLE, FACEBOOK, APPLE, GITHUB, TWITTER */
    @Column(name = "auth_provider", length = 30)
    private String authProvider = "EMAIL";

    // Sugestão: Adicionar Role para facilitar o JWT depois
    @Column(length = 20)
    private String role = "ROLE_CUSTOMER";

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // Campos de imagem
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "image_url_public_id", length = 255)
    private String imageUrlPublicId;

    @Convert(converter = SensitiveLocalDateConverter.class)
    @Column(name = "birth_date", length = 128)
    private LocalDate birthDate;

    @Lob
    @Column(name = "onboarding_progress_json", columnDefinition = "LONGTEXT")
    private String onboardingProgressJson;

    @PrePersist
    @PreUpdate
    private void updatePrivacyIndexes() {
        this.emailHash = PrivacyHash.emailHash(this.email);
    }

    @ElementCollection
    @CollectionTable(
            name = "customer_favorite_barbershops",
            joinColumns = @JoinColumn(name = "customer_id"),
            uniqueConstraints = {
                    @UniqueConstraint(name = "uk_customer_favorite_shop", columnNames = {"customer_id", "barbershop_id"})
            }
    )
    @Column(name = "barbershop_id", nullable = false, columnDefinition = "VARCHAR(36)")
    @JdbcTypeCode(Types.VARCHAR)
    private Set<UUID> favoriteBarbershopIds = new HashSet<>();
}
