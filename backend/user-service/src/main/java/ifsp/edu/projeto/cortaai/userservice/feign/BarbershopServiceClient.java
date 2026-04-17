package ifsp.edu.projeto.cortaai.userservice.feign;

import ifsp.edu.projeto.cortaai.userservice.dto.BarbershopInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "barbershop-service",
        path = "/api/barbershops",
        fallbackFactory = BarbershopServiceClientFallbackFactory.class
)
public interface BarbershopServiceClient {

    @GetMapping("/{id}")
    BarbershopInfoDTO getBarbershopById(@PathVariable("id") UUID id);
}
