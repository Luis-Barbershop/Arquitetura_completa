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

    @Value("${springdoc.server-relative-url:/}")
    private String relativeServerUrl;

    @Value("${SPRINGDOC_SERVER_URL:}")
    private String publicServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        // Default to same-origin to avoid browser calls to Docker-internal IPs.
        servers.add(new Server().url(relativeServerUrl).description("Same origin"));

        if (StringUtils.hasText(publicServerUrl)) {
            servers.add(new Server().url(publicServerUrl).description("Public gateway"));
        }

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
