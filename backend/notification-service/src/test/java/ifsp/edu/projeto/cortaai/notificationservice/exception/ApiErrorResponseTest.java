package ifsp.edu.projeto.cortaai.notificationservice.exception;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorResponseTest {

    @Test
    void shouldExposeErrorResponseFields() {
        ApiErrorResponse.FieldError fieldError = new ApiErrorResponse.FieldError("token", "obrigatório");
        ApiErrorResponse response = new ApiErrorResponse(
                400,
                "Bad Request",
                "Payload inválido",
                "MethodArgumentNotValidException",
                "notification-service",
                "/api/notifications",
                "cid-123",
                List.of(fieldError)
        );

        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getError()).isEqualTo("Bad Request");
        assertThat(response.getMessage()).isEqualTo("Payload inválido");
        assertThat(response.getCause()).isEqualTo("MethodArgumentNotValidException");
        assertThat(response.getOrigin()).isEqualTo("notification-service");
        assertThat(response.getPath()).isEqualTo("/api/notifications");
        assertThat(response.getCorrelationId()).isEqualTo("cid-123");
        assertThat(response.getFieldErrors()).containsExactly(fieldError);
        assertThat(fieldError.field()).isEqualTo("token");
        assertThat(fieldError.message()).isEqualTo("obrigatório");
    }
}
