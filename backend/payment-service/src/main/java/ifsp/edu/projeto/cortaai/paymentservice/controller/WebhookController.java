package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para receber webhooks do Mercado Pago.
 * Endpoint público (sem autenticação) — validação via payload.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "NOVO: Endpoints públicos para recebimento de notificações assíncronas de plataformas externas (Mercado Pago)")
public class WebhookController {

    private final PaymentService paymentService;

    /**
     * Recebe notificação (webhook) do Mercado Pago.
     * Formato: { "action": "payment.created", "data": { "id": "12345" }, "type": "payment" }
     */
    @Operation(summary = "Receber notificação do Mercado Pago", description = "Endpoint público (sem auth) chamado automaticamente pelo Mercado Pago quando há atualização no status de um pagamento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook processado ou ignorado com sucesso (retorna 200 sempre para evitar retentativas infinitas do MP)")
    })
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody Map<String, Object> payload) {
        log.info("Webhook recebido (Mercado Pago)");

        try {
            String type = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null || data.get("id") == null) {
                log.warn("Webhook sem data.id, ignorando");
                return ResponseEntity.ok().build();
            }

            String resourceId = data.get("id").toString();

            if (!paymentService.isWebhookTrusted(resourceId, xSignature, xRequestId)) {
                log.warn("Webhook rejeitado por validacao de assinatura/replay: resourceId={}, type={}", resourceId, type);
                return ResponseEntity.ok().build();
            }

            paymentService.processWebhook(resourceId, type, buildWebhookAuditPayload(payload, resourceId));

        } catch (Exception e) {
            log.error("Erro ao processar webhook do Mercado Pago", e);
            // Retornar 200 mesmo com erro para MP não reenviar indefinidamente
        }

        return ResponseEntity.ok().build();
    }

    private String buildWebhookAuditPayload(Map<String, Object> payload, String resourceId) {
        String type = String.valueOf(payload.getOrDefault("type", ""));
        String action = String.valueOf(payload.getOrDefault("action", ""));
        return String.format("{type=%s, action=%s, data.id=%s}", type, action, resourceId);
    }
}