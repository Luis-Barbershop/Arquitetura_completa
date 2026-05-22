package ifsp.edu.projeto.cortaai.barbershopservice.config;

import com.cloudinary.Cloudinary;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BarbershopConfigTest {

    @Test
    void shouldCreateOpenApiMetadataWithBearerSecurity() {
        OpenApiConfig config = new OpenApiConfig();
        ReflectionTestUtils.setField(config, "publicServerUrl", "https://api.cortaai.test");

        OpenAPI openAPI = config.customOpenAPI();

        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("https://api.cortaai.test");
        assertThat(openAPI.getInfo().getTitle()).contains("Barbershop Service");
        assertThat(openAPI.getComponents().getSecuritySchemes())
                .containsKey("Firebase Bearer Token");
        assertThat(openAPI.getSecurity().get(0)).containsKey("Firebase Bearer Token");
    }

    @Test
    void shouldCreateRabbitBeans() {
        RabbitConfig config = new RabbitConfig();
        MessageConverter converter = config.jsonMessageConverter();

        assertThat(config.cortaaiEventsExchange().getName()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(converter).isNotNull();
        assertThat(config.rabbitTemplate(mock(ConnectionFactory.class), converter).getMessageConverter())
                .isSameAs(converter);
    }

    @Test
    void shouldCreateCloudinaryClientWithConfiguredCredentials() {
        CloudinaryConfig config = new CloudinaryConfig();
        ReflectionTestUtils.setField(config, "cloudName", "demo-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "key-123");
        ReflectionTestUtils.setField(config, "apiSecret", "secret-123");

        Cloudinary cloudinary = config.cloudinary();

        assertThat(cloudinary.config.cloudName).isEqualTo("demo-cloud");
        assertThat(cloudinary.config.apiKey).isEqualTo("key-123");
        assertThat(cloudinary.config.apiSecret).isEqualTo("secret-123");
        assertThat(cloudinary.config.secure).isTrue();
    }
}
