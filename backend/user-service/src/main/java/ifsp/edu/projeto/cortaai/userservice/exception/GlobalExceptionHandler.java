package ifsp.edu.projeto.cortaai.userservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler global de exceções do user-service.
 * 
 * Converte exceções em respostas JSON legíveis em vez do 500 genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ─── 404 — Recurso não encontrado ─────────────────────────────────────────
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // ─── 400 — Validação do @Valid falhou ─────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validação falhou: {}", errors);
        return buildResponse(HttpStatus.BAD_REQUEST, errors);
    }

    // ─── 409 — Constraint UNIQUE violada (CPF, email, telefone duplicado) ─────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = extractConstraintMessage(ex);
        log.warn("Violação de integridade: {}", message);
        return buildResponse(HttpStatus.CONFLICT, message);
    }

    // ─── 401 — Token Firebase inválido ────────────────────────────────────────
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException ex) {
        log.warn("Erro de segurança: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // ─── 400 — Header obrigatório ausente (X-User-UID) ───────────────────────
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Header obrigatório ausente: {}", ex.getHeaderName());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Header obrigatório ausente: " + ex.getHeaderName() +
                ". Verifique se a requisição passou pelo API Gateway.");
    }

    // ─── 400 — Argumentos ilegais ─────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Argumento inválido: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ─── 409 — Registro inconsistente (StaleState / OptimisticLocking) ────────
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleStaleState(ObjectOptimisticLockingFailureException ex) {
        log.warn("Registro inconsistente (possível dado de migração corrompido): {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "Registro inconsistente no banco de dados. O administrador precisa limpar os dados corrompidos.");
    }

    // ─── 500 — Qualquer outra exceção ─────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Erro interno não tratado: {} — {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno: " + ex.getClass().getSimpleName() + " — " + ex.getMessage());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Tenta extrair uma mensagem amigável da violação de constraint.
     * Ex.: "Duplicate entry '12345678901' for key 'customers.document_cpf'"
     */
    private String extractConstraintMessage(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage == null) {
            return "Violação de integridade de dados.";
        }

        // MySQL: "Duplicate entry 'X' for key 'table.column'"
        if (rootMessage.contains("Duplicate entry")) {
            if (rootMessage.contains("document_cpf")) {
                return "Este CPF já está cadastrado.";
            }
            if (rootMessage.contains("email")) {
                return "Este e-mail já está cadastrado.";
            }
            if (rootMessage.contains("tell")) {
                return "Este telefone já está cadastrado.";
            }
            if (rootMessage.contains("firebase_uid")) {
                return "Este usuário Firebase já está vinculado a outra conta.";
            }
            return "Registro duplicado: " + rootMessage;
        }

        return "Erro de integridade de dados: " + rootMessage;
    }
}
