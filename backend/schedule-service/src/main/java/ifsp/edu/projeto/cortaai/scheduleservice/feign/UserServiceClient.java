package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserInfoDTO getUserById(@PathVariable("id") UUID id);

    @GetMapping("/by-email/{email}")
    UserInfoDTO getUserByEmail(@PathVariable("email") String email);
}

