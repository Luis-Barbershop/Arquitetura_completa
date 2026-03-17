package ifsp.edu.projeto.cortaai.barbershopservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DomainConflictException extends RuntimeException {
    public DomainConflictException() { super(); }
    public DomainConflictException(String message) { super(message); }
}

