package ifsp.edu.projeto.cortaai.scheduleservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

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
    @Column(nullable = false, updatable = false, length = 36)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "activity_id", nullable = false, length = 36)
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

