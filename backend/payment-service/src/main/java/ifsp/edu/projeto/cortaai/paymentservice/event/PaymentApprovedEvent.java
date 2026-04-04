package ifsp.edu.projeto.cortaai.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento publicado quando um pagamento é aprovado.
 * Consumido pelo notification-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovedEvent {
    private UUID transactionId;
    private UUID appointmentId;
    private UUID customerId;
    private String customerEmail;
    private BigDecimal amount;
}
