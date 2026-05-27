package ifsp.edu.projeto.cortaai.scheduleservice.model.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "v_barber_skill_matrix")
@IdClass(VBarberSkillMatrix.VBarberSkillMatrixId.class)
@Getter
public class VBarberSkillMatrix {

    @Id
    @Column(name = "barber_id")
    private String barberId;

    @Id
    @Column(name = "activity_name")
    private String activityName;

    @Column(name = "barbershop_id")
    private String barbershopId;

    @Column(name = "barber_name")
    private String barberName;

    @Column(name = "times_executed")
    private Long timesExecuted;

    @Column(name = "total_generated_by_activity")
    private BigDecimal totalGeneratedByActivity;

    public static class VBarberSkillMatrixId implements Serializable {
        private String barberId;
        private String activityName;
    }
}
