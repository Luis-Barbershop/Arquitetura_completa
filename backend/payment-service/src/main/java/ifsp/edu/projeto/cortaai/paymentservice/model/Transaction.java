package ifsp.edu.projeto.cortaai.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Campos adicionados para conciliação financeira do Marketplace:
 * - grossAmount:       valor bruto (o que o cliente pagou)
 * - netAmount:         valor líquido repassado ao barbeiro
 * - mpFeeAmount:       taxa cobrada pelo Mercado Pago
 * - platformFeeAmount: taxa da plataforma CortaAI
 * - paymentMethod:     PIX, CREDIT_CARD, etc.
 */

/**
 * Entidade que representa uma transação de pagamento.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID appointmentId;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID customerId;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID barbershopId;

    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * Valor bruto pago pelo cliente (= amount antes do split).
     * Preenchido após aprovação do pagamento via webhook.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal grossAmount;

    /**
     * Valor líquido repassado ao barbeiro após descontar taxas.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal netAmount;

    /**
     * Taxa cobrada pelo Mercado Pago sobre a transação.
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal mpFeeAmount;

    /**
     * Taxa da plataforma CortaAI (application fee no split).
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal platformFeeAmount;

    /**
     * Método de pagamento: PIX, CREDIT_CARD, DEBIT_CARD, LOCAL, etc.
     */
    @Column(length = 30)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * ID da preferência no Mercado Pago.
     */
    @Column(unique = true)
    private String mpPreferenceId;

    /**
     * ID do pagamento no Mercado Pago (retornado no webhook).
     */
    private String mpPaymentId;

    /**
     * URL de checkout do Mercado Pago.
     */
    @Column(columnDefinition = "TEXT")
    private String checkoutUrl;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
