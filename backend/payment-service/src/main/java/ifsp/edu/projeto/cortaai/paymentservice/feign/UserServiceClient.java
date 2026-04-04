package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.SaveMpCredentialsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign Client para comunicação interna com o user-service.
 * Usado para salvar as credenciais Mercado Pago do barbeiro após o OAuth.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @PutMapping("/api/internal/users/barbers/{barberId}/mp-credentials")
    void saveMpCredentials(@PathVariable("barberId") UUID barberId,
                           @RequestBody SaveMpCredentialsDTO dto);
}
