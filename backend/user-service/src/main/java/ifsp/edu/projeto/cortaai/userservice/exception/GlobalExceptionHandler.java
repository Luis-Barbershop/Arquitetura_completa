package ifsp.edu.projeto.cortaai.userservice.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.UUID;

/**
 * Handler global de exceções do user-service.
 * <p>
 * Todas as respostas de erro seguem o formato {@link ApiErrorResponse} padronizado
 * com: timestamp, status, error, message, cause, origin, path, correlationId.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Value("${spring.application.name:user-service}")
    private String serviceName;

    // ─── 403 — Proibido ───────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado: {} em {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, "Acesso negado: " + ex.getMessage(), ex, request);
    }

    // ─── 404 — Não encontrado ─────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entidade JPA não encontrada: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("Nenhum handler encontrado: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return buildResponse(HttpStatus.NOT_FOUND, "Endpoint não encontrado: " + ex.getRequestURL(), ex, request);
    }

    // ─── 400 — Dados inválidos ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String summary = fieldErrors.stream()
                .map(fe -> fe.field() + ": " + fe.message())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Dados inválidos.");
        log.warn("Validação falhou: {}", summary);
        return buildResponse(HttpStatus.BAD_REQUEST, summary, ex, request, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        log.warn("Parte multipart ausente: {} em {}", ex.getRequestPartName(), request.getRequestURI());
        return buildResponse(HttpStatus.BAD_REQUEST, "Parte obrigatória ausente: " + ex.getRequestPartName(), ex, request);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Header obrigatório ausente: {}", ex.getHeaderName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Header obrigatório ausente: " + ex.getHeaderName() + ". Verifique se a requisição passou pelo API Gateway.",
                ex, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), ex, request);
    }

    // ─── 401 — Segurança ──────────────────────────────────────────────────────

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurity(SecurityException ex, HttpServletRequest request) {
        log.warn("Erro de segurança: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), ex, request);
    }

    // ─── 409 — Conflito ───────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = extractConstraintMessage(ex);
        log.warn("Violação de integridade: {}", message);
        return buildResponse(HttpStatus.CONFLICT, message, ex, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        log.warn("Estado ilegal: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), ex, request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleStaleState(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Registro inconsistente (optimistic lock): {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "Registro inconsistente no banco de dados. Tente novamente.", ex, request);
    }

    // ─── 415 — Tipo de mídia não suportado ────────────────────────────────────

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Content-Type não suportado: {} em {}", ex.getMessage(), request.getRequestURI());
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type não suportado: " + ex.getContentType(), ex, request);
    }

    // ─── 500 — Catch-all ──────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Exceção não tratada em {}: {} — {}", request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno: " + ex.getClass().getSimpleName() + " — " + ex.getMessage(), ex, request);
    }

    // ─── Builders ─────────────────────────────────────────────────────────────

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
                                                           Exception ex, HttpServletRequest request) {
        return buildResponse(status, message, ex, request, null);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
                                                           Exception ex, HttpServletRequest request,
                                                           List<ApiErrorResponse.FieldError> fieldErrors) {
        ApiErrorResponse body = new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                ex.getClass().getSimpleName(),
                serviceName,
                request.getRequestURI(),
                resolveCorrelationId(request),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String resolveCorrelationId(HttpServletRequest request) {
        if (request == null) return UUID.randomUUID().toString();
        String cid = request.getHeader("X-Correlation-Id");
        if (cid != null && !cid.isBlank()) return cid;
        Object attr = request.getAttribute("correlationId");
        if (attr instanceof String v && !v.isBlank()) return v;
        return UUID.randomUUID().toString();
    }

    /**
     * Tenta extrair uma mensagem amigável da violação de constraint.
     * Ex.: "Duplicate entry '12345678901' for key 'customers.document_cpf'"
     */
    private String extractConstraintMessage(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage == null) return "Violação de integridade de dados.";
        if (rootMessage.contains("Duplicate entry")) {
            if (rootMessage.contains("document_cpf")) return "Este CPF já está cadastrado.";
            if (rootMessage.contains("email"))        return "Este e-mail já está cadastrado.";
            if (rootMessage.contains("tell"))         return "Este telefone já está cadastrado.";
            if (rootMessage.contains("firebase_uid")) return "Este usuário Firebase já está vinculado a outra conta.";
            return "Registro duplicado: " + rootMessage;
        }
        return "Erro de integridade de dados: " + rootMessage;
    }
}
