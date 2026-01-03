package ifsp.edu.projeto.cortaai.userservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

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

    // Sugestão: Role explícita (além do isOwner) ajuda na segurança do Spring
    @Column(length = 20)
    private String role = "ROLE_BARBER";

    // NOVO: Referência à Barbearia apenas pelo ID
    // (Pode ser Null se ele ainda não estiver vinculado a nenhuma barbearia)
    @Column(name = "barbershop_id")
    private Long barbershopId; // Ou UUID, se a Barbearia também for usar UUID

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
}