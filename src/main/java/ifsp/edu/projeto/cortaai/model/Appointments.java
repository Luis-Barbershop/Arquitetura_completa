package ifsp.edu.projeto.cortaai.model;

import ifsp.edu.projeto.cortaai.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.model.enums.BarberSkills;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "Appointments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Appointments {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private OffsetDateTime datetime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id")
    private Barber barber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;


}
