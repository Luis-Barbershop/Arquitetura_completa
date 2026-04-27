package ifsp.edu.projeto.cortaai.discoveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.port=0",
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
})
class DiscoveryServiceApplicationTest {

    @Test
    void shouldLoadContext() {
        // smoke test de contexto
    }

    @Test
    void shouldKeepEurekaServerAnnotation() {
        assertTrue(DiscoveryServiceApplication.class.isAnnotationPresent(EnableEurekaServer.class));
    }
}
