package ifsp.edu.projeto.cortaai.notificationservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign Client para o user-service — resolve Firebase UID → UUID do banco.
 */
@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserServiceClient {

    @GetMapping("/by-firebase-uid/{uid}")
    UserInfoDTO getUserByFirebaseUid(@PathVariable("uid") String uid);
}
