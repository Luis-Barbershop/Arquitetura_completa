package ifsp.edu.projeto.cortaai.barbershopservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "Firebase Bearer Token";

    @Value("${SPRINGDOC_SERVER_URL:http://localhost:8080}")
    private String publicServerUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url(publicServerUrl).description("API Gateway"));

        return new OpenAPI()
                .servers(servers)
                .info(new Info()
                        .title("Barbershop Service API — CortaAí")
                        .description("""
                                Serviço de barbearias da plataforma CortaAí.
                                
                                ## Autenticação
                                Todos os endpoints protegidos usam **Firebase Authentication** via API Gateway.
                                O Gateway valida o token e injeta headers `X-User-UID`, `X-User-Email`, `X-User-Type`.
                                
                                ## Fluxo de registro de barbearia
                                1. `POST /api/auth/verify` — verifica token Firebase (user-service)
                                2. `POST /api/auth/barbers/complete-profile` — completa perfil do barbeiro (user-service)
                                3. `POST /api/barbershops/register-my-shop` — cria a barbearia (este serviço)
                                
                                ## Endpoints internos
                                Os endpoints em `/api/internal/barbershops` são para comunicação inter-serviço e NÃO são expostos pelo Gateway.
                                """)
                        .version("2.0")
                        .contact(new Contact()
                                .name("CortaAí Team")
                                .url("https://github.com/AppCortaAi")))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Firebase ID Token")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}
