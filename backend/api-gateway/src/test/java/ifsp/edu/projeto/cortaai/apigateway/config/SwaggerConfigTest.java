package ifsp.edu.projeto.cortaai.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    @Test
    void shouldRegisterSwaggerUrlsForGatewayServices() {
        SwaggerUiConfigProperties properties = new SwaggerUiConfigProperties();
        SwaggerConfig config = new SwaggerConfig();

        List<SwaggerUrl> urls = config.apis(properties, null);

        assertThat(urls).hasSize(6);
        assertThat(urls)
                .extracting(SwaggerUrl::getName)
                .containsExactly(
                        "user-service",
                        "barbershop-service",
                        "schedule-service",
                        "payment-service",
                        "notification-service",
                        "product-service"
                );
        assertThat(urls)
                .extracting(SwaggerUrl::getUrl)
                .contains("/v3/api-docs/user-service", "/v3/api-docs/product-service");
        assertThat(properties.getUrls()).containsExactlyInAnyOrderElementsOf(urls);
    }
}
