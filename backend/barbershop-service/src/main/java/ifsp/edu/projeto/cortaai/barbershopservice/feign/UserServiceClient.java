package ifsp.edu.projeto.cortaai.barbershopservice.feign;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign Client para comunicação com o user-service.
 * Usa os endpoints internos (/api/internal/users).
 */
@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserInfoDTO getUserById(@PathVariable("id") UUID id);

    @GetMapping("/by-email/{email}")
    UserInfoDTO getUserByEmail(@PathVariable("email") String email);

    @PutMapping("/{id}/barbershop")
    void updateUserBarbershopId(@PathVariable("id") UUID id, @RequestBody UUID barbershopId);
}

