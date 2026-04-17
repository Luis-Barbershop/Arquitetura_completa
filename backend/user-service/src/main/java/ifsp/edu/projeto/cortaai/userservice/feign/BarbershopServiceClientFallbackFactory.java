package ifsp.edu.projeto.cortaai.userservice.feign;

import ifsp.edu.projeto.cortaai.userservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ExternalServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BarbershopServiceClientFallbackFactory implements FallbackFactory<BarbershopServiceClient> {

    @Override
    public BarbershopServiceClient create(Throwable cause) {
        return new BarbershopServiceClient() {
            @Override
            public BarbershopInfoDTO getBarbershopById(UUID id) {
                String suffix = (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank())
                        ? " Causa: " + cause.getMessage()
                        : "";
                throw new ExternalServiceUnavailableException("barbershop-service indisponível no momento." + suffix);
            }
        };
    }
}
