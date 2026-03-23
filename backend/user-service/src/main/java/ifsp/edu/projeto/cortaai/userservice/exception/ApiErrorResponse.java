package ifsp.edu.projeto.cortaai.userservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Resposta padronizada de erro — identica em todos os microserviços CortaAí.
 * <p>
 * Campos obrigatórios: timestamp, status, error, message, origin, path, correlationId.
 * Campos opcionais: cause, fieldErrors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta padronizada de erro da API CortaAí")
public class ApiErrorResponse {

    @Schema(description = "Momento exato do erro (ISO-8601/UTC)", example = "2025-06-30T18:30:00Z")
    private final Instant timestamp;

    @Schema(description = "Código HTTP", example = "404")
    private final int status;

    @Schema(description = "Razão HTTP (ex: Not Found)", example = "Not Found")
    private final String error;

    @Schema(description = "Mensagem legível explicando o erro", example = "Usuário não encontrado.")
    private final String message;

    @Schema(description = "Classe da exceção que originou o erro", example = "NotFoundException")
    private final String cause;

    @Schema(description = "Microserviço que gerou o erro", example = "user-service")
    private final String origin;

    @Schema(description = "URI que causou o erro", example = "/api/barbers/123")
    private final String path;

    @Schema(description = "ID de correlação para rastreio entre serviços", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private final String correlationId;

    @Schema(description = "Lista de erros de validação por campo (apenas para 400)")
    private final List<FieldError> fieldErrors;

    public ApiErrorResponse(int status, String error, String message, String cause,
                            String origin, String path, String correlationId,
                            List<FieldError> fieldErrors) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.cause = cause;
        this.origin = origin;
        this.path = path;
        this.correlationId = correlationId;
        this.fieldErrors = fieldErrors;
    }

    /** Construtor simplificado sem fieldErrors. */
    public ApiErrorResponse(int status, String error, String message, String cause,
                            String origin, String path, String correlationId) {
        this(status, error, message, cause, origin, path, correlationId, null);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getCause() { return cause; }
    public String getOrigin() { return origin; }
    public String getPath() { return path; }
    public String getCorrelationId() { return correlationId; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }

    // ─── FieldError interno ───────────────────────────────────────────────────

    @Schema(description = "Detalhe de erro de validação de um campo específico")
    public record FieldError(
            @Schema(description = "Nome do campo", example = "email")
            String field,
            @Schema(description = "Mensagem de erro do campo", example = "não pode ser vazio")
            String message
    ) {}
}
