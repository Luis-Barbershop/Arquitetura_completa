package ifsp.edu.projeto.cortaai.apigateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class SwaggerConfig {

    @Bean
    @Lazy(false)
    public List<SwaggerUrl> apis(SwaggerUiConfigProperties swaggerUiConfigProperties,
                                 RouteLocator routeLocator) {
        List<SwaggerUrl> urls = new ArrayList<>();

        // Definir os serviços que possuem documentação OpenAPI
        urls.add(new SwaggerUrl("user-service", "/v3/api-docs/user-service", "User Service"));
        urls.add(new SwaggerUrl("barbershop-service", "/v3/api-docs/barbershop-service", "Barbershop Service"));
        urls.add(new SwaggerUrl("schedule-service", "/v3/api-docs/schedule-service", "Schedule Service"));
        urls.add(new SwaggerUrl("payment-service", "/v3/api-docs/payment-service", "Payment Service"));
        urls.add(new SwaggerUrl("notification-service", "/v3/api-docs/notification-service", "Notification Service"));
        urls.add(new SwaggerUrl("product-service", "/v3/api-docs/product-service", "Product Service"));

        swaggerUiConfigProperties.setUrls(new java.util.HashSet<>(urls));

        return urls;
    }
}
