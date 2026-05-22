package ifsp.edu.projeto.cortaai.barbershopservice.exception;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "serviceName", "barbershop-service");
        request = new MockHttpServletRequest("POST", "/api/barbershops");
        request.addHeader("X-Correlation-Id", "cid-shop");
    }

    @Test
    void shouldMapForbiddenAndAccessDenied() {
        assertError(handler.handleForbidden(new ForbiddenException("sem permissão"), request),
                HttpStatus.FORBIDDEN, "sem permissão", "ForbiddenException");
        assertError(handler.handleAccessDenied(new AccessDeniedException("bloqueado"), request),
                HttpStatus.FORBIDDEN, "Acesso negado: bloqueado", "AccessDeniedException");
    }

    @Test
    void shouldMapNotFoundVariants() throws Exception {
        assertError(handler.handleNotFound(new NotFoundException("barbearia não encontrada"), request),
                HttpStatus.NOT_FOUND, "barbearia não encontrada", "NotFoundException");
        assertError(handler.handleEntityNotFound(new EntityNotFoundException("atividade não encontrada"), request),
                HttpStatus.NOT_FOUND, "atividade não encontrada", "EntityNotFoundException");
        assertError(handler.handleNoHandler(new NoHandlerFoundException("GET", "/missing", new HttpHeaders()), request),
                HttpStatus.NOT_FOUND, "Endpoint não encontrado: /missing", "NoHandlerFoundException");
    }

    @Test
    void shouldMapBadRequestAndSecurityErrors() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("valor inválido"), request),
                HttpStatus.BAD_REQUEST, "valor inválido", "IllegalArgumentException");
        assertError(handler.handleSecurity(new SecurityException("credencial inválida"), request),
                HttpStatus.UNAUTHORIZED, "credencial inválida", "SecurityException");
    }

    @Test
    void shouldMapConflictVariants() {
        assertError(handler.handleDomainConflict(new DomainConflictException("cnpj já existe"), request),
                HttpStatus.CONFLICT, "cnpj já existe", "DomainConflictException");
        assertError(handler.handleIllegalState(new IllegalStateException("estado inválido"), request),
                HttpStatus.CONFLICT, "estado inválido", "IllegalStateException");
    }

    @Test
    void shouldTranslateDataIntegrityMessages() {
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'barbershops.cnpj'"), request)
                .getBody().getMessage()).isEqualTo("Este CNPJ já está cadastrado.");
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'email'"), request)
                .getBody().getMessage()).isEqualTo("Este e-mail já está cadastrado.");
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'tell'"), request)
                .getBody().getMessage()).isEqualTo("Este telefone já está cadastrado.");
        assertThat(handler.handleDataIntegrity(violation("Duplicate entry 'x' for key 'other'"), request)
                .getBody().getMessage()).startsWith("Registro duplicado:");
        assertThat(handler.handleDataIntegrity(violation("constraint failed"), request)
                .getBody().getMessage()).isEqualTo("Erro de integridade de dados: constraint failed");
    }

    @Test
    void shouldMapUnavailableAndGenericErrors() {
        assertError(handler.handleUserServiceUnavailable(new UserServiceUnavailableException("user-service fora"), request),
                HttpStatus.SERVICE_UNAVAILABLE, "user-service fora", "UserServiceUnavailableException");
        assertError(handler.handleGenericException(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: RuntimeException — boom", "RuntimeException");
    }

    @Test
    void shouldUseRequestAttributeCorrelationIdWhenHeaderIsMissing() {
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/barbershops/1");
        requestWithoutHeader.setAttribute("correlationId", "attr-shop");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new NotFoundException("não achei"), requestWithoutHeader);

        assertThat(response.getBody().getCorrelationId()).isEqualTo("attr-shop");
        assertThat(response.getBody().getPath()).isEqualTo("/api/barbershops/1");
    }

    private DataIntegrityViolationException violation(String rootMessage) {
        return new DataIntegrityViolationException("integrity", new RuntimeException(rootMessage));
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
        assertThat(response.getBody().getOrigin()).isEqualTo("barbershop-service");
        assertThat(response.getBody().getPath()).isEqualTo("/api/barbershops");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("cid-shop");
    }
}
