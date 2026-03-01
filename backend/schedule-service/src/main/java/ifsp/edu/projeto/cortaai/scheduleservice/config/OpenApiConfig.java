package ifsp.edu.projeto.cortaai.scheduleservice.config;

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
                        .title("Schedule Service API")
                        .description("Serviço de agendamentos — criação, consulta, cancelamento e conclusão de agendamentos")
                        .version("1.0")
                        .contact(new Contact()
                                .name("CortaAí")
                                .url("https://github.com/AppCortaAi")));
    }
}
