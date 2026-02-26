package ifsp.edu.projeto.cortaai.schedule.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", fallback = UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping("/api/barbers/{id}")
    BarberDTO getBarberById(@PathVariable("id") UUID id);

    @GetMapping("/api/customers/{id}")
    CustomerDTO getCustomerById(@PathVariable("id") UUID id);

    record BarberDTO(UUID id, String name, String email, UUID barbershopId) {}
    record CustomerDTO(UUID id, String name, String email) {}
}
