package ifsp.edu.projeto.cortaai.userservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "serviceName", "user-service");
        request = new MockHttpServletRequest("GET", "/api/users/me");
        request.addHeader("X-Correlation-Id", "cid-123");
    }

    @Test
    void shouldMapAccessDeniedToForbidden() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(new AccessDeniedException("sem acesso"), request);

        assertError(response, HttpStatus.FORBIDDEN, "Acesso negado: sem acesso", "AccessDeniedException");
    }

    @Test
    void shouldMapRoleConflictToForbidden() {
        ResponseEntity<ApiErrorResponse> response = handler.handleRoleConflict(
                new RoleConflictException("perfil incompatível", "BARBER"), request);

        assertError(response, HttpStatus.FORBIDDEN, "perfil incompatível", "RoleConflictException");
    }

    @Test
    void shouldMapNotFoundVariantsToNotFound() throws Exception {
        assertError(handler.handleNotFound(new NotFoundException("não achei"), request),
                HttpStatus.NOT_FOUND, "não achei", "NotFoundException");
        assertError(handler.handleEntityNotFound(new EntityNotFoundException("entidade sumiu"), request),
                HttpStatus.NOT_FOUND, "entidade sumiu", "EntityNotFoundException");
        assertError(handler.handleNoHandler(new NoHandlerFoundException("GET", "/missing", new HttpHeaders()), request),
                HttpStatus.NOT_FOUND, "Endpoint não encontrado: /missing", "NoHandlerFoundException");
    }

    @Test
    void shouldMapBadRequestSecurityAndUnavailableErrors() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("campo ruim"), request),
                HttpStatus.BAD_REQUEST, "campo ruim", "IllegalArgumentException");
        assertError(handler.handleSecurity(new SecurityException("token inválido"), request),
                HttpStatus.UNAUTHORIZED, "token inválido", "SecurityException");
        assertError(handler.handleExternalUnavailable(new ExternalServiceUnavailableException("firebase fora"), request),
                HttpStatus.SERVICE_UNAVAILABLE, "firebase fora", "ExternalServiceUnavailableException");
    }

    @Test
    void shouldMapDuplicateCpfIntegrityViolationToConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "duplicate", new RuntimeException("Duplicate entry '123' for key 'customers.document_cpf'"));

        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrity(exception, request);

        assertError(response, HttpStatus.CONFLICT, "Este CPF já está cadastrado.", "DataIntegrityViolationException");
    }

    @Test
    void shouldMapDuplicateEmailTellFirebaseAndUnknownIntegrityViolations() {
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'customers.email'"), request)
                .getBody().getMessage()).isEqualTo("Este e-mail já está cadastrado.");
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'customers.tell'"), request)
                .getBody().getMessage()).isEqualTo("Este telefone já está cadastrado.");
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'customers.firebase_uid'"), request)
                .getBody().getMessage()).isEqualTo("Este usuário Firebase já está vinculado a outra conta.");
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'other'"), request)
                .getBody().getMessage()).startsWith("Registro duplicado:");
    }

    @Test
    void shouldMapIllegalStateAndGenericErrors() {
        assertError(handler.handleIllegalState(new IllegalStateException("estado ruim"), request),
                HttpStatus.CONFLICT, "estado ruim", "IllegalStateException");
        assertError(handler.handleGeneric(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: RuntimeException — boom", "RuntimeException");
    }

    @Test
    void shouldUseRequestAttributeCorrelationIdWhenHeaderIsMissing() {
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/users");
        requestWithoutHeader.setAttribute("correlationId", "attr-cid");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new NotFoundException("não achei"), requestWithoutHeader);

        assertThat(response.getBody().getCorrelationId()).isEqualTo("attr-cid");
        assertThat(response.getBody().getPath()).isEqualTo("/api/users");
    }

    private DataIntegrityViolationException violation(String rootMessage) {
        return new DataIntegrityViolationException("integrity", new RuntimeException(rootMessage));
    }

    @Test
    void shouldHandleValidationWithFieldErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "customer");
        bindingResult.addError(new org.springframework.validation.FieldError("customer", "name", "Nome obrigatório"));
        MethodParameter mp = new MethodParameter(Object.class.getDeclaredMethod("hashCode"), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("name").contains("Nome obrigatório");
        assertThat(response.getBody().getFieldErrors()).hasSize(1);
        assertThat(response.getBody().getFieldErrors().get(0).field()).isEqualTo("name");
        assertThat(response.getBody().getFieldErrors().get(0).message()).isEqualTo("Nome obrigatório");
    }

    @Test
    void shouldHandleMissingPartStaleStateAndMissingHeader() throws NoSuchMethodException {
        ResponseEntity<ApiErrorResponse> r1 = handler.handleMissingPart(
                new MissingServletRequestPartException("foto"), request);
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r1.getBody().getMessage()).contains("foto");

        ResponseEntity<ApiErrorResponse> r2 = handler.handleStaleState(
                new ObjectOptimisticLockingFailureException(Object.class, "id-123"), request);
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(r2.getBody().getMessage()).isEqualTo("Registro inconsistente no banco de dados. Tente novamente.");

        MethodParameter mp = new MethodParameter(Object.class.getDeclaredMethod("hashCode"), -1);
        MissingRequestHeaderException headerEx = new MissingRequestHeaderException("X-User-Id", mp);
        ResponseEntity<ApiErrorResponse> r3 = handler.handleMissingHeader(headerEx, request);
        assertThat(r3.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(r3.getBody().getMessage()).contains("X-User-Id");
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
        assertThat(response.getBody().getOrigin()).isEqualTo("user-service");
        assertThat(response.getBody().getPath()).isEqualTo("/api/users/me");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("cid-123");
    }
}
