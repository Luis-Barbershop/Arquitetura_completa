package ifsp.edu.projeto.cortaai.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Evento recebido quando um pagamento é aprovado.
 * Publicado pelo payment-service (Dev 2).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentApprovedEvent {
    private UUID transactionId;
    private UUID appointmentId;
    private UUID customerId;
    private BigDecimal amount;
}
