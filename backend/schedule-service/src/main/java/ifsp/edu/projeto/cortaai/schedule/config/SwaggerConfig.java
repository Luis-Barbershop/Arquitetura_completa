package ifsp.edu.projeto.cortaai.schedule.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Schedule Service API")
                        .description("API do microserviço de agendamentos do CortaAí")
                        .version("0.1")
                        .contact(new Contact()
                                .name("CortaAí Team")
                                .email("contato@cortaai.com")));
    }
}
