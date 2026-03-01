package ifsp.edu.projeto.cortaai.paymentservice.model;

/**
 * Status de uma transação de pagamento.
 */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    REFUNDED,
    IN_PROCESS
}
