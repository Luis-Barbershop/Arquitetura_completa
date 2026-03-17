package ifsp.edu.projeto.cortaai.barbershopservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException() { super(); }
    public UserServiceUnavailableException(String message) { super(message); }
}

