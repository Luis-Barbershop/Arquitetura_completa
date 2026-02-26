package ifsp.edu.projeto.cortaai.schedule.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "barbershop-service", fallback = BarbershopServiceClientFallback.class)
public interface BarbershopServiceClient {

    @GetMapping("/api/barbershops/{id}")
    BarbershopDTO getBarbershopById(@PathVariable("id") UUID id);

    record BarbershopDTO(UUID id, String name, String cnpj, UUID ownerId) {}
}
