package ifsp.edu.projeto.cortaai.barbershopservice.feign;

import ifsp.edu.projeto.cortaai.barbershopservice.exception.UserServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserServiceClientFallbackFactoryTest {

    @Test
    void shouldThrowUnavailableWithOriginalCauseMessageForEveryFallbackMethod() {
        UserServiceClient client = new UserServiceClientFallbackFactory()
                .create(new RuntimeException("timeout"));
        UUID id = UUID.randomUUID();

        assertUnavailable(() -> client.getUserById(id), "timeout");
        assertUnavailable(() -> client.getUserByEmail("ana@example.com"), "timeout");
        assertUnavailable(() -> client.getUserByFirebaseUid("firebase-uid"), "timeout");
        assertUnavailable(() -> client.updateUserBarbershopId(id, Map.of("barbershopId", id.toString())), "timeout");
        assertUnavailable(() -> client.getBarbersByBarbershop(id), "timeout");
        assertUnavailable(() -> client.getBarberByCpf(Map.of("cpf", "12345678909")), "timeout");
        assertUnavailable(() -> client.makeBarberOwner("firebase-uid"), "timeout");
    }

    @Test
    void shouldThrowUnavailableWithoutSuffixWhenCauseHasNoMessage() {
        UserServiceClient client = new UserServiceClientFallbackFactory().create(new RuntimeException());

        assertThatThrownBy(() -> client.getUserByFirebaseUid("firebase-uid"))
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessage("user-service indisponível no momento.");
    }

    private void assertUnavailable(Runnable invocation, String causeMessage) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(UserServiceUnavailableException.class)
                .hasMessageContaining("user-service indisponível no momento.")
                .hasMessageContaining(causeMessage);
    }
}
