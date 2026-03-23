package ifsp.edu.projeto.cortaai.scheduleservice.exception;

/**
 * Exceção lançada quando um recurso não é encontrado.
 * O status HTTP 404 é definido pelo GlobalExceptionHandler.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException() { super(); }
    public NotFoundException(String message) { super(message); }
}

