package ifsp.edu.projeto.cortaai.scheduleservice.model;

import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @Column(nullable = false, updatable = false, columnDefinition = "varchar(36)")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    // --- IDs desacoplados (vivem em outros serviços) ---

    @Column(name = "customer_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID customerId;

    @Column(name = "barber_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID barberId;

    @Column(name = "barbershop_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID barbershopId;

    // --- Dados desnormalizados (snapshots copiados na criação) ---

    @Column(name = "customer_name", nullable = false, length = 70)
    private String customerName;

    @Column(name = "barber_name", nullable = false, length = 70)
    private String barberName;

    @Column(name = "barbershop_name", nullable = false, length = 255)
    private String barbershopName;

    // --- Horário ---

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // --- Valor ---

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // --- Status ---

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    // --- Auditoria ---

    @CreatedDate
    @Column(name = "date_created", nullable = false, updatable = false)
    private LocalDateTime dateCreated;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // --- Atividades do agendamento (snapshots) ---

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AppointmentActivity> activities = new HashSet<>();
}