package ifsp.edu.projeto.cortaai.paymentservice.model.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_barber_financial_performance")
@Getter
public class VBarberFinancialPerformance {

    @Id
    @Column(name = "barber_id")
    private String barberId;

    @Column(name = "barber_name")
    private String barberName;

    @Column(name = "barbershop_id")
    private UUID barbershopId;

    @Column(name = "total_appointments")
    private Long totalAppointments;

    @Column(name = "generated_revenue")
    private BigDecimal generatedRevenue;

    @Column(name = "contribution_percentage")
    private BigDecimal contributionPercentage;
}
