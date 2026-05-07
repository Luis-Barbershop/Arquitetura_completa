package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.MpConnectionStatusDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.SaveMpCredentialsDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign Client para comunicação interna com o user-service.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @PutMapping("/api/internal/users/barbers/{barberId}/mp-credentials")
    void saveMpCredentials(@PathVariable("barberId") UUID barberId,
                           @RequestBody SaveMpCredentialsDTO dto);

    @GetMapping("/api/internal/users/{userId}")
    UserInfoDTO getUserById(@PathVariable("userId") UUID userId);

    @GetMapping("/api/internal/users/by-firebase-uid/{uid}")
    UserInfoDTO getUserByFirebaseUid(@PathVariable("uid") String uid);

    @GetMapping("/api/internal/users/barbers/{barberId}/mp-status")
    MpConnectionStatusDTO getBarberMpStatus(@PathVariable("barberId") UUID barberId);

    @GetMapping("/api/internal/users/barbers/{barberId}/mp-credentials")
    SaveMpCredentialsDTO getBarberMpCredentials(@PathVariable("barberId") UUID barberId);

    @PutMapping("/api/internal/users/barbers/{barberId}/mp-disconnect")
    void disconnectBarberMp(@PathVariable("barberId") UUID barberId);
}
