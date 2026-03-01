package ifsp.edu.projeto.cortaai.paymentservice.dto;

import ifsp.edu.projeto.cortaai.paymentservice.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de retorno de uma transação.
 */
public record TransactionDTO(
        UUID id,
        UUID appointmentId,
        UUID customerId,
        BigDecimal amount,
        PaymentStatus status,
        String checkoutUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
