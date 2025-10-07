package ifsp.edu.projeto.cortaai.model;

import ifsp.edu.projeto.cortaai.model.enums.BarberSkills;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "Barbers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Barber {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "char(36)")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 70)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String tell;

    @Column(nullable = false, unique = true, length = 70)
    private String email;

    @Column(nullable = false, unique = true, length = 11)
    private String documentCPF;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BarberSkills mainSkill;

    @Column
    @Enumerated(EnumType.STRING)
    private BarberSkills secondSkill;

    @Column
    @Enumerated(EnumType.STRING)
    private BarberSkills thirdSkill;

    @Column(nullable = false, columnDefinition = "char(36)")
    private UUID barberShop;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

}
