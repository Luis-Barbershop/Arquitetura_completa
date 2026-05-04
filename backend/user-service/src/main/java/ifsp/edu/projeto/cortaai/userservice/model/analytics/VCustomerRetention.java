package ifsp.edu.projeto.cortaai.userservice.model.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "v_customer_retention")
@Getter
public class VCustomerRetention {

    @Id
    @Column(name = "reference_month")
    private String referenceMonth;

    @Column(name = "returning_customers")
    private Long returningCustomers;
}
