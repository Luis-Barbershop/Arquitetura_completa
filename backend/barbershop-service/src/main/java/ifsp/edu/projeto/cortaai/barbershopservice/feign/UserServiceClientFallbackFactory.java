package ifsp.edu.projeto.cortaai.barbershopservice.feign;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.exception.UserServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.List;

@Component
public class UserServiceClientFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        return new UserServiceClient() {
            @Override
            public UserInfoDTO getUserById(UUID id) {
                throw unavailable(cause);
            }

            @Override
            public UserInfoDTO getUserByEmail(String email) {
                throw unavailable(cause);
            }

            @Override
            public UserInfoDTO getUserByFirebaseUid(String uid) {
                throw unavailable(cause);
            }

            @Override
            public void updateUserBarbershopId(UUID id, Map<String, String> body) {
                throw unavailable(cause);
            }

            @Override
            public List<UserInfoDTO> getBarbersByBarbershop(UUID barbershopId) {
                throw unavailable(cause);
            }

            @Override
            public UserInfoDTO getBarberByCpf(String cpf) {
                throw unavailable(cause);
            }

            @Override
            public ResponseEntity<Void> makeBarberOwner(String uid) {
                throw unavailable(cause);
            }
        };
    }

    private UserServiceUnavailableException unavailable(Throwable cause) {
        String suffix = (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank())
                ? " Causa: " + cause.getMessage()
                : "";
        return new UserServiceUnavailableException(
                "user-service indisponível no momento." + suffix
        );
    }
}

