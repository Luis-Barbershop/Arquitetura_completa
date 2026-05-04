package ifsp.edu.projeto.cortaai.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryServiceApplicationTest {

    @Test
    void shouldKeepSpringBootApplicationAnnotation() {
        assertTrue(DiscoveryServiceApplication.class.isAnnotationPresent(SpringBootApplication.class));
    }

    @Test
    void shouldKeepEurekaServerAnnotation() {
        assertTrue(DiscoveryServiceApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }
}
