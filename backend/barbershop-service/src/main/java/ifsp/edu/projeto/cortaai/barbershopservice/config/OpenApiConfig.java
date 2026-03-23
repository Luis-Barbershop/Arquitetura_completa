package ifsp.edu.projeto.cortaai.barbershopservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    // URL pública do gateway (ex: https://api.cortaai.shop) injetada via env SPRINGDOC_SERVER_URL
    @Value("${SPRINGDOC_SERVER_URL:http://localhost:8080}")
    private String publicServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();

        // Gateway sempre primeiro — Swagger "Try it out" usa o primeiro servidor da lista
        servers.add(new Server().url(publicServerUrl).description("API Gateway"));

        return new OpenAPI()
                .servers(servers)
                .info(new Info()
                        .title("Barbershop Service API")
                        .description("Serviço de barbearias — CRUD de barbearias, serviços oferecidos e horários")
                        .version("1.0")
                        .contact(new Contact()
                                .name("CortaAí")
                                .url("https://github.com/AppCortaAi")));
    }
}
