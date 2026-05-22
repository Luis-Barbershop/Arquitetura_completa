package ifsp.edu.projeto.cortaai.productservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.util.List;

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
    void shouldMapValidationAndMissingHeaderErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "product");
        bindingResult.addError(new FieldError("product", "name", "não pode ser vazio"));
        bindingResult.addError(new FieldError("product", "price", "deve ser positivo"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class);
        MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(
                new MethodParameter(method, 0),
                bindingResult
        );

        ResponseEntity<ApiErrorResponse> validationResponse = handler.handleValidation(validationException, request);

        assertError(validationResponse, HttpStatus.BAD_REQUEST,
                "name: não pode ser vazio; price: deve ser positivo", "MethodArgumentNotValidException");
        assertThat(validationResponse.getBody().getFieldErrors()).hasSize(2);

        MissingRequestHeaderException headerException = new MissingRequestHeaderException(
                "X-User-Id",
                new MethodParameter(method, 0)
        );

        assertError(handler.handleMissingHeader(headerException, request),
                HttpStatus.BAD_REQUEST,
                "Header obrigatório ausente: X-User-Id. Verifique se a requisição passou pelo API Gateway.",
                "MissingRequestHeaderException");
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
    void shouldMapUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN,
                List.of(MediaType.APPLICATION_JSON)
        );

        assertError(handler.handleMediaType(exception, request),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type não suportado: text/plain",
                "HttpMediaTypeNotSupportedException");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/products/2");

        ResponseEntity<ApiErrorResponse> response = handler.handleEntityNotFound(
                new EntityNotFoundException("não achei"), requestWithoutHeader);

        assertThat(response.getBody().getCorrelationId()).isNotBlank();
        assertThat(response.getBody().getPath()).isEqualTo("/api/products/2");
    }

    @Test
    void shouldExposeApiErrorFieldsIncludingValidationErrors() {
        ApiErrorResponse.FieldError fieldError = new ApiErrorResponse.FieldError("name", "não pode ser vazio");
        ApiErrorResponse response = new ApiErrorResponse(
                400,
                "Bad Request",
                "Dados inválidos",
                "MethodArgumentNotValidException",
                "product-service",
                "/api/products",
                "cid-validation",
                List.of(fieldError)
        );

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getError()).isEqualTo("Bad Request");
        assertThat(response.getMessage()).isEqualTo("Dados inválidos");
        assertThat(response.getCause()).isEqualTo("MethodArgumentNotValidException");
        assertThat(response.getOrigin()).isEqualTo("product-service");
        assertThat(response.getPath()).isEqualTo("/api/products");
        assertThat(response.getCorrelationId()).isEqualTo("cid-validation");
        assertThat(response.getFieldErrors()).containsExactly(fieldError);
        assertThat(fieldError.field()).isEqualTo("name");
        assertThat(fieldError.message()).isEqualTo("não pode ser vazio");
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

    @SuppressWarnings("unused")
    private void validationTarget(String value) {
    }
}
