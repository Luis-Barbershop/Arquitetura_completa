package ifsp.edu.projeto.cortaai.productservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "serviceName", "product-service");
        request = new MockHttpServletRequest("PATCH", "/api/products/1");
        request.addHeader("X-Correlation-Id", "cid-product");
    }

    @Test
    void shouldMapEntityAndRouteNotFound() throws Exception {
        assertError(handler.handleEntityNotFound(new EntityNotFoundException("produto não encontrado"), request),
                HttpStatus.NOT_FOUND, "produto não encontrado", "EntityNotFoundException");
        assertError(handler.handleNoHandler(new NoHandlerFoundException("GET", "/missing", new HttpHeaders()), request),
                HttpStatus.NOT_FOUND, "Endpoint não encontrado: /missing", "NoHandlerFoundException");
    }

    @Test
    void shouldMapBadRequestAndConflictErrors() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("quantidade inválida"), request),
                HttpStatus.BAD_REQUEST, "quantidade inválida", "IllegalArgumentException");
        assertError(handler.handleIllegalState(new IllegalStateException("estoque insuficiente"), request),
                HttpStatus.CONFLICT, "estoque insuficiente", "IllegalStateException");
    }

    @Test
    void shouldMapDataIntegrityAndGenericErrors() {
        assertError(handler.handleDataIntegrity(
                        new DataIntegrityViolationException("integrity", new RuntimeException("unique product failed")), request),
                HttpStatus.CONFLICT, "Violação de integridade de dados: unique product failed", "DataIntegrityViolationException");
        assertError(handler.handleGeneric(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: RuntimeException — boom", "RuntimeException");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/products/2");

        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(
                new EntityNotFoundException("não achei"), requestWithoutHeader);

        assertThat(response.getBody().getCorrelationId()).isNotBlank();
        assertThat(response.getBody().getPath()).isEqualTo("/api/products/2");
    }

    private void assertError(ResponseEntity<ApiErrorResponse> response,
                             HttpStatus status,
                             String message,
                             String cause) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(status.value());
        assertThat(response.getBody().getMessage()).isEqualTo(message);
        assertThat(response.getBody().getCause()).isEqualTo(cause);
        assertThat(response.getBody().getOrigin()).isEqualTo("product-service");
        assertThat(response.getBody().getPath()).isEqualTo("/api/products/1");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("cid-product");
    }
}
