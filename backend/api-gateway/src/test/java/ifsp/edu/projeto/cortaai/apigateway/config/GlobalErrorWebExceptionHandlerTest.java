package ifsp.edu.projeto.cortaai.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalErrorWebExceptionHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GlobalErrorWebExceptionHandler handler = new GlobalErrorWebExceptionHandler();

    @Test
    void shouldRenderResponseStatusExceptionWithIncomingCorrelationId() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header("X-Correlation-Id", "cid-status")
                        .build());

        handler.handle(exchange, new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo inválido")).block();

        Map<String, Object> body = responseBody(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isEqualTo("cid-status");
        assertThat(body).containsEntry("status", 400)
                .containsEntry("error", "Bad Request")
                .containsEntry("message", "Campo inválido")
                .containsEntry("cause", "ResponseStatusException")
                .containsEntry("origin", "api-gateway")
                .containsEntry("path", "/api/appointments")
                .containsEntry("correlationId", "cid-status");
    }

    @Test
    void shouldMapConnectionFailuresToServiceUnavailableAndUseAttributeCorrelationId() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/barbershops").build());
        exchange.getAttributes().put("correlationId", "cid-attr");

        handler.handle(exchange, new ConnectException("connection refused")).block();

        Map<String, Object> body = responseBody(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(body).containsEntry("status", 503)
                .containsEntry("message", "Serviço de destino indisponível. Tente novamente em alguns instantes.")
                .containsEntry("correlationId", "cid-attr");
    }

    @Test
    void shouldMapTimeoutNamedExceptionsToGatewayTimeout() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/schedules").build());

        handler.handle(exchange, new DownstreamTimeoutException()).block();

        Map<String, Object> body = responseBody(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(body).containsEntry("status", 504)
                .containsEntry("message", "O serviço de destino não respondeu a tempo.");
        assertThat((String) body.get("correlationId")).isNotBlank();
    }

    @Test
    void shouldMapUnexpectedErrorsToBadGateway() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build());

        handler.handle(exchange, new IllegalStateException("boom")).block();

        Map<String, Object> body = responseBody(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(body).containsEntry("status", 502)
                .containsEntry("message", "Serviço de destino indisponível. Tente novamente em alguns instantes.")
                .containsEntry("cause", "IllegalStateException");
    }

    @Test
    void shouldNotOverwriteCommittedResponses() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/stream").build());
        exchange.getResponse().setComplete().block();
        IllegalStateException failure = new IllegalStateException("already committed");

        assertThatThrownBy(() -> handler.handle(exchange, failure).block())
                .isSameAs(failure);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> responseBody(MockServerWebExchange exchange) throws Exception {
        return MAPPER.readValue(exchange.getResponse().getBodyAsString().block(), Map.class);
    }

    private static class DownstreamTimeoutException extends RuntimeException {
    }
}
