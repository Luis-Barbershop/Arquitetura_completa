package ifsp.edu.projeto.cortaai.productservice.model;

/**
 * Status de um pedido.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    DELIVERED,
    CANCELLED
}
