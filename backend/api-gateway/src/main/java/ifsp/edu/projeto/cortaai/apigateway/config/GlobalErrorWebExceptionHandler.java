package ifsp.edu.projeto.cortaai.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handler global de erros do API Gateway.
 * <p>
 * Captura erros de roteamento (serviço indisponível, timeout, etc.)
 * e retorna o mesmo formato padronizado {@code ApiErrorResponse} dos microserviços.
 */
@Component
@Order(-1) // Prioridade máxima (antes do DefaultErrorWebExceptionHandler do Spring Boot)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Determina o status HTTP adequado
        HttpStatus status = resolveStatus(ex);

        // Não sobrescreve se já estiver committed (streaming, websocket, etc.)
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        String correlationId = resolveCorrelationId(exchange);
        String path = exchange.getRequest().getURI().getPath();
        String message = resolveMessage(ex, status);

        log.error("event=gateway-error status={} path={} correlationId={} cause={} message={}",
                status.value(), path, correlationId, ex.getClass().getSimpleName(), message);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("cause", ex.getClass().getSimpleName());
        body.put("origin", "api-gateway");
        body.put("path", path);
        body.put("correlationId", correlationId);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);

        try {
            byte[] bytes = mapper.writeValueAsBytes(body);
            var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception jsonEx) {
            // Fallback — se Jackson falhar, monta à mão
            String fallback = "{\"error\":\"" + status.getReasonPhrase() + "\",\"message\":\"" + message + "\",\"origin\":\"api-gateway\"}";
            var buffer = exchange.getResponse().bufferFactory().wrap(fallback.getBytes());
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        if (ex instanceof ConnectException
                || ex.getClass().getName().contains("ServiceUnavailable")
                || ex.getClass().getName().contains("RetryExhausted")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (ex.getClass().getName().contains("TimeoutException")) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Serviço de destino indisponível. Tente novamente em alguns instantes.";
        }
        if (status == HttpStatus.GATEWAY_TIMEOUT) {
            return "O serviço de destino não respondeu a tempo.";
        }
        String msg = ex.getMessage();
        return msg != null ? msg : "Erro no gateway ao encaminhar a requisição.";
    }

    private String resolveCorrelationId(ServerWebExchange exchange) {
        String cid = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        if (cid != null && !cid.isBlank()) return cid;
        Object attr = exchange.getAttribute("correlationId");
        if (attr instanceof String v && !v.isBlank()) return v;
        return UUID.randomUUID().toString();
    }
}
