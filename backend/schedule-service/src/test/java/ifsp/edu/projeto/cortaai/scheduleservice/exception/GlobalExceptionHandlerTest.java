package ifsp.edu.projeto.cortaai.scheduleservice.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldMapValidationErrorsWithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "startTime", "não pode ser nulo"));
        bindingResult.addError(new FieldError("target", "barberId", "obrigatório"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body.getFieldErrors()).hasSize(2);
        assertThat(body.getMessage()).contains("startTime").contains("barberId");
        assertThat(body.getFieldErrors().get(0).field()).isNotBlank();
        assertThat(body.getFieldErrors().get(0).message()).isNotBlank();
    }

    @Test
    void shouldMapMissingRequestHeader() {
        MissingRequestHeaderException ex = new MissingRequestHeaderException("X-User-Id",
                mock(org.springframework.core.MethodParameter.class));

        ResponseEntity<ApiErrorResponse> response = handler.handleMissingHeader(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("X-User-Id");
    }

    @Test
    void shouldMapPessimisticLockingAsConcurrencyConflict() {
        assertError(handler.handleConcurrency(new PessimisticLockingFailureException("lock timeout"), request),
                HttpStatus.CONFLICT,
                "Não foi possível reservar o horário porque outro agendamento concorrente ocorreu. Tente novamente.",
                "PessimisticLockingFailureException");
    }

    @Test
    void shouldMapNotReadableExceptionGenericCase() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad body",
                new RuntimeException("unexpected"), null);

        ResponseEntity<ApiErrorResponse> response = handler.handleNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .isEqualTo("Corpo da requisição inválido ou campo com valor não reconhecido.");
    }

    @Test
    void shouldMapNotReadableExceptionWithEnumHint() {
        com.fasterxml.jackson.databind.exc.InvalidFormatException ife =
                mock(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
        when(ife.getTargetType()).thenAnswer(inv -> AppointmentStatusEnum.class);

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad enum", ife, null);

        ResponseEntity<ApiErrorResponse> response = handler.handleNotReadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Valores aceitos");
    }

    @Test
    void shouldMapUnsupportedMediaType() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("text/xml");

        ResponseEntity<ApiErrorResponse> response = handler.handleMediaType(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().getMessage()).contains("Content-Type não suportado");
    }

    @Test
    void shouldCoverExceptionConstructorsWithCause() {
        assertThat(new NotFoundException("msg")).hasMessage("msg");
        assertThat(new NotFoundException()).hasMessage(null);
        assertThat(new ConflictException("msg")).hasMessage("msg");
        assertThat(new ConflictException()).hasMessage(null);
        assertThat(new ifsp.edu.projeto.cortaai.scheduleservice.exception.ForbiddenException("denied")).hasMessage("denied");
    }

    @Test
    void shouldExposeApiErrorResponseGetters() {
        ApiErrorResponse body = handler.handleNotFound(new NotFoundException("x"), request).getBody();
        assertThat(body.getStatus()).isEqualTo(404);
        assertThat(body.getError()).isEqualTo("Not Found");
        assertThat(body.getMessage()).isEqualTo("x");
        assertThat(body.getCause()).isEqualTo("NotFoundException");
        assertThat(body.getOrigin()).isEqualTo("schedule-service");
        assertThat(body.getPath()).isEqualTo("/api/appointments");
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getFieldErrors()).isNull();
    }

    /** Enum auxiliar para teste de handleNotReadable com hint de enum. */
    enum AppointmentStatusEnum { SCHEDULED, CANCELLED }

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
