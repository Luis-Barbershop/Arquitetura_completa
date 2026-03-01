package ifsp.edu.projeto.cortaai.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("Serviço de pagamentos — integração Mercado Pago, criação e consulta de pagamentos")
                        .version("1.0")
                        .contact(new Contact()
                                .name("CortaAí")
                                .url("https://github.com/AppCortaAi")));
    }
}
