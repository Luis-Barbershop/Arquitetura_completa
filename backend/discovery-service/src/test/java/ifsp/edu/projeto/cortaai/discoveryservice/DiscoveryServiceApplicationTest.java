package ifsp.edu.projeto.cortaai.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DiscoveryServiceApplicationTest {

    @Test
    void shouldKeepSpringBootApplicationAnnotation() {
        assertTrue(DiscoveryServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

    @Test
    void shouldKeepEurekaServerAnnotation() {
        assertTrue(DiscoveryServiceApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }

    @Test
    void shouldInstantiateApplicationClass() {
        assertDoesNotThrow(DiscoveryServiceApplication::new);
    }

    @Test
    void shouldStartApplicationWithNonWebTestProfile() {
        assertDoesNotThrow(() -> DiscoveryServiceApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.main.lazy-initialization=true",
                "--spring.autoconfigure.exclude=org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration,org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration"
        }));
    }
}
