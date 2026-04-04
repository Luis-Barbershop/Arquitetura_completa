package ifsp.edu.projeto.cortaai.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * DTO para criar um pagamento.
 * paymentMethod: "PIX" ou "CREDIT_CARD"
 */
public record CreatePaymentDTO(
        @NotNull UUID appointmentId,
        @Pattern(regexp = "PIX|CREDIT_CARD|LOCAL", message = "Método inválido. Use: PIX, CREDIT_CARD ou LOCAL")
        String paymentMethod
) {}
