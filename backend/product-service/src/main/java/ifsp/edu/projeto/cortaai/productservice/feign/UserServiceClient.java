package ifsp.edu.projeto.cortaai.productservice.feign;

import ifsp.edu.projeto.cortaai.productservice.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/internal/users/by-firebase-uid/{uid}")
    UserInfoDTO getUserByFirebaseUid(@PathVariable("uid") String uid);
}
