package ifsp.edu.projeto.cortaai.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.sql.Types;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Customer {

    @Id
    @Column(nullable = false, updatable = false, length = 36)
    @GeneratedValue
    @JdbcTypeCode(Types.VARCHAR)
    @UuidGenerator
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
}