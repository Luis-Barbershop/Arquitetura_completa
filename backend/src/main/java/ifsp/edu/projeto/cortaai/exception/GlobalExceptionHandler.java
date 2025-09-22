package ifsp.edu.projeto.cortaai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Lida com exceções de "Recurso Não Encontrado".
     * Retorna um status HTTP 404 (Not Found).
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleResourceNotFoundException(NotFoundException ex) {
        // Você pode retornar um objeto JSON personalizado aqui se preferir.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Lida com exceções de "Referência".
     * Retorna um status HTTP 409 (Conflict).
     */
    @ExceptionHandler(ReferenceException.class)
    public ResponseEntity<String> handleReferenceException(ReferenceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    /**
     * Lida com qualquer outra exceção não mapeada.
     * Retorna um status HTTP 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ocorreu um erro inesperado: " + ex.getMessage());
    }
}