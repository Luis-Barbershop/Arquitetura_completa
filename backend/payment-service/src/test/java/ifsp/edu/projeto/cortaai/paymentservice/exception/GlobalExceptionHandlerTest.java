package ifsp.edu.projeto.cortaai.paymentservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "serviceName", "payment-service");
        request = new MockHttpServletRequest("POST", "/api/payments/create");
        request.addHeader("X-Correlation-Id", "cid-payment");
    }

    @Test
    void shouldMapNotFoundVariants() throws Exception {
        assertError(handler.handleEntityNotFound(new EntityNotFoundException("pagamento não encontrado"), request),
                HttpStatus.NOT_FOUND, "pagamento não encontrado", "EntityNotFoundException");
        assertError(handler.handleNoHandler(new NoHandlerFoundException("GET", "/missing", new HttpHeaders()), request),
                HttpStatus.NOT_FOUND, "Endpoint não encontrado: /missing", "NoHandlerFoundException");
    }

    @Test
    void shouldMapBadRequestErrors() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("payload inválido"), request),
                HttpStatus.BAD_REQUEST, "payload inválido", "IllegalArgumentException");
        assertError(handler.handleMissingHeader(new MissingRequestHeaderException("X-User-Id", null), request),
                HttpStatus.BAD_REQUEST,
                "Header obrigatório ausente: X-User-Id. Verifique se a requisição passou pelo API Gateway.",
                "MissingRequestHeaderException");
    }

    @Test
    void shouldMapConflictAndUnsupportedMediaType() {
        assertError(handler.handleIllegalState(new IllegalStateException("pagamento já processado"), request),
                HttpStatus.CONFLICT, "pagamento já processado", "IllegalStateException");
        assertError(handler.handleMediaType(new HttpMediaTypeNotSupportedException(
                        MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON)), request),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type não suportado: text/plain",
                "HttpMediaTypeNotSupportedException");
    }

    @Test
    void shouldMapGenericErrors() {
        assertError(handler.handleGeneric(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: RuntimeException — boom", "RuntimeException");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/payments/1");

        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(
                new EntityNotFoundException("não achei"), requestWithoutHeader);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotBlank();
        assertThat(response.getBody().getPath()).isEqualTo("/api/payments/1");
    }

    private void assertError(ResponseEntity<ApiErrorResponse> response,
                             HttpStatus status,
                             String message,
                             String cause) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(status.value());
        assertThat(response.getBody().getError()).isEqualTo(status.getReasonPhrase());
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getCause()).isEqualTo(cause);
        assertThat(response.getBody().getOrigin()).isEqualTo("payment-service");
        assertThat(response.getBody().getPath()).isEqualTo("/api/payments/create");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("cid-payment");
    }
}
