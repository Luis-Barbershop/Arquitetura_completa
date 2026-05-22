package ifsp.edu.projeto.cortaai.apigateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void shouldInstantiateDisabledCorsConfiguration() {
        assertThat(new CorsConfig()).isNotNull();
    }
}
