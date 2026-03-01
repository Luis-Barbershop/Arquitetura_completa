package ifsp.edu.projeto.cortaai.notificationservice.config;

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
                        .title("Notification Service API")
                        .description("Serviço de notificações — envio de e-mails e consulta de notificações")
                        .version("1.0")
                        .contact(new Contact()
                                .name("CortaAí")
                                .url("https://github.com/AppCortaAi")));
    }
}
