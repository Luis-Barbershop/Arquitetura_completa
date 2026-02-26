package ifsp.edu.projeto.cortaai.schedule.client;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public BarberDTO getBarberById(UUID id) {
        return new BarberDTO(id, "Unknown", "unknown@email.com", null);
    }

    @Override
    public CustomerDTO getCustomerById(UUID id) {
        return new CustomerDTO(id, "Unknown", "unknown@email.com");
    }
}
