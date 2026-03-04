package ifsp.edu.projeto.cortaai.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "Firebase Bearer Token";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API — CortaAí")
                        .description("""
                                Serviço de usuários da plataforma CortaAí.
                                
                                ## Autenticação
                                Esta API usa **Firebase Authentication**.
                                
                                **Como obter o token:**
                                1. Autentique o usuário no app com o Firebase SDK (Google, Facebook, Apple, e-mail, etc.)
                                2. Obtenha o ID Token: `user.getIdToken()`
                                3. Clique em **Authorize** e cole o token no campo (sem o prefixo `Bearer`)
                                
                                **Exceção:** `POST /api/auth/verify` é público — o token vai no **corpo** da requisição.
                                """)
                        .version("2.0")
                        .contact(new Contact()
                                .name("CortaAí Team")
                                .url("https://github.com/AppCortaAi")))

                // Servidor padrão (via Gateway)
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway (dev)"),
                        new Server().url("http://localhost:8081").description("User Service direto (dev)")
                ))

                // Declara o esquema Bearer para aparecer o botão "Authorize" no Swagger UI
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Firebase ID Token")
                                .description("Cole o Firebase ID Token obtido pelo SDK cliente (sem o prefixo 'Bearer')")))

                // Aplica o esquema globalmente em todos os endpoints
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME));
    }
}

