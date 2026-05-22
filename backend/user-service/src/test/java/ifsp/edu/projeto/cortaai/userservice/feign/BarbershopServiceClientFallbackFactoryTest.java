package ifsp.edu.projeto.cortaai.userservice.feign;

import ifsp.edu.projeto.cortaai.userservice.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarbershopServiceClientFallbackFactoryTest {

    @Test
    void shouldThrowUnavailableExceptionWithCauseMessage() {
        BarbershopServiceClient fallback = new BarbershopServiceClientFallbackFactory()
                .create(new RuntimeException("timeout"));

        assertThatThrownBy(() -> fallback.getBarbershopById(UUID.randomUUID()))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessageContaining("barbershop-service indisponível")
                .hasMessageContaining("timeout");
    }

    @Test
    void shouldThrowUnavailableExceptionWithoutBlankCause() {
        BarbershopServiceClient fallback = new BarbershopServiceClientFallbackFactory()
                .create(new RuntimeException(" "));

        assertThatThrownBy(() -> fallback.getBarbershopById(UUID.randomUUID()))
                .isInstanceOf(ExternalServiceUnavailableException.class)
                .hasMessage("barbershop-service indisponível no momento.");
    }
}
