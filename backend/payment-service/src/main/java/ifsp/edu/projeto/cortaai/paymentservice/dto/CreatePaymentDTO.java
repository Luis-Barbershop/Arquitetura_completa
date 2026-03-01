package ifsp.edu.projeto.cortaai.paymentservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * DTO para criar um pagamento.
 */
public record CreatePaymentDTO(
        @NotNull UUID appointmentId
) {}
