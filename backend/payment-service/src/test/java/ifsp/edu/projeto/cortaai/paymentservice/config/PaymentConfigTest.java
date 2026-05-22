package ifsp.edu.projeto.cortaai.paymentservice.config;

import com.mercadopago.MercadoPagoConfig;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PaymentConfigTest {

    @Test
    void shouldBuildOpenApiMetadata() {
        OpenAPI openAPI = new OpenApiConfig().customOpenAPI();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Payment Service API");
        assertThat(openAPI.getInfo().getDescription()).contains("Mercado Pago");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0");
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("CortaAí");
    }

    @Test
    void shouldBuildRabbitInfrastructureBeans() {
        RabbitConfig config = new RabbitConfig();

        TopicExchange exchange = config.exchange();
        Queue queue = config.customerDeletedQueue();
        Binding binding = config.bindCustomerDeleted();
        RabbitTemplate template = config.rabbitTemplate(mock(ConnectionFactory.class));

        assertThat(exchange.getName()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(queue.getName()).isEqualTo(RabbitConfig.QUEUE_CUSTOMER_DELETED);
        assertThat(binding.getExchange()).isEqualTo(RabbitConfig.EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitConfig.RK_CUSTOMER_DELETED);
        assertThat(config.jsonMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
        assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void shouldConfigureMercadoPagoAccessToken() {
        MercadoPagoConfiguration configuration = new MercadoPagoConfiguration();
        ReflectionTestUtils.setField(configuration, "accessToken", "test-token");

        configuration.init();

        assertThat(MercadoPagoConfig.getAccessToken()).isEqualTo("test-token");
    }
}
