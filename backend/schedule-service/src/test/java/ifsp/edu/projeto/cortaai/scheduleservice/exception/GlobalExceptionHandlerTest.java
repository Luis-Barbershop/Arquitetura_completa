package ifsp.edu.projeto.cortaai.scheduleservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
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
        ReflectionTestUtils.setField(handler, "serviceName", "schedule-service");
        request = new MockHttpServletRequest("POST", "/api/appointments");
        request.addHeader("X-Correlation-Id", "cid-schedule");
    }

    @Test
    void shouldMapNotFoundVariants() throws Exception {
        assertError(handler.handleNotFound(new NotFoundException("agendamento não encontrado"), request),
                HttpStatus.NOT_FOUND, "agendamento não encontrado", "NotFoundException");
        assertError(handler.handleNoHandler(new NoHandlerFoundException("GET", "/missing", new HttpHeaders()), request),
                HttpStatus.NOT_FOUND, "Endpoint não encontrado: /missing", "NoHandlerFoundException");
    }

    @Test
    void shouldMapBadRequestAndForbidden() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("payload inválido"), request),
                HttpStatus.BAD_REQUEST, "payload inválido", "IllegalArgumentException");
        assertError(handler.handleForbidden(new ForbiddenException("sem permissão"), request),
                HttpStatus.FORBIDDEN, "sem permissão", "ForbiddenException");
    }

    @Test
    void shouldMapConflictAndConcurrencyErrors() {
        assertError(handler.handleConflict(new ConflictException("horário ocupado"), request),
                HttpStatus.CONFLICT, "horário ocupado", "ConflictException");
        assertError(handler.handleIllegalState(new IllegalStateException("estado ruim"), request),
                HttpStatus.CONFLICT, "estado ruim", "IllegalStateException");
        assertError(handler.handleConcurrency(new CannotAcquireLockException("lock"), request),
                HttpStatus.CONFLICT,
                "Não foi possível reservar o horário porque outro agendamento concorrente ocorreu. Tente novamente.",
                "CannotAcquireLockException");
    }

    @Test
    void shouldMapDataIntegrityAndGenericErrors() {
        assertError(handler.handleDataIntegrity(
                        new DataIntegrityViolationException("integrity", new RuntimeException("foreign key failed")), request),
                HttpStatus.CONFLICT, "Violação de integridade de dados: foreign key failed", "DataIntegrityViolationException");
        assertError(handler.handleGeneric(new RuntimeException("boom"), request),
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno: RuntimeException — boom", "RuntimeException");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing() {
        MockHttpServletRequest requestWithoutHeader = new MockHttpServletRequest("GET", "/api/appointments/1");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new NotFoundException("não achei"), requestWithoutHeader);

        assertThat(response.getBody().getCorrelationId()).isNotBlank();
        assertThat(response.getBody().getPath()).isEqualTo("/api/appointments/1");
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
        assertThat(response.getBody().getOrigin()).isEqualTo("schedule-service");
        assertThat(response.getBody().getPath()).isEqualTo("/api/appointments");
        assertThat(response.getBody().getCorrelationId()).isEqualTo("cid-schedule");
    }
}
