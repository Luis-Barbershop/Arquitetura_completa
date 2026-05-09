package ifsp.edu.projeto.cortaai.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Log de webhooks recebidos do Mercado Pago.
 * Usado para idempotência — evitar processar o mesmo webhook 2x.
 */
@Entity
@Table(name = "webhook_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    /**
     * ID do recurso no Mercado Pago (ex: payment_id).
     */
    @Column(nullable = false, unique = true)
    private String mpResourceId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Builder.Default
    private boolean processed = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;
}
