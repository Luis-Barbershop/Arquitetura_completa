package ifsp.edu.projeto.cortaai.scheduleservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "appointment_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentActivity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "varchar(36)")
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "activity_id", nullable = false, columnDefinition = "varchar(36)")
    private UUID activityId;

    @Column(name = "activity_name", nullable = false, length = 255)
    private String activityName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;
}

