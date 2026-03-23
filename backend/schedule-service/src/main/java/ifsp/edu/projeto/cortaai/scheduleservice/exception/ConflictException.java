package ifsp.edu.projeto.cortaai.scheduleservice.exception;

/**
 * Exceção lançada quando há conflito (ex: horário já ocupado).
 * O status HTTP 409 é definido pelo GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {
    public ConflictException() { super(); }
    public ConflictException(String message) { super(message); }
}

