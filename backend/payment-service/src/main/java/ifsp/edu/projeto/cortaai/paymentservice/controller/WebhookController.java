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
            @RequestBody Map<String, Object> payload) {
        log.info("Webhook recebido: {}", payload);

        try {
            String type = (String) payload.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null || data.get("id") == null) {
                log.warn("Webhook sem data.id, ignorando");
                return ResponseEntity.ok().build();
            }

            String resourceId = data.get("id").toString();
            paymentService.processWebhook(resourceId, type, payload.toString());

        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage());
            // Retornar 200 mesmo com erro para MP não reenviar indefinidamente
        }

        return ResponseEntity.ok().build();
    }
}