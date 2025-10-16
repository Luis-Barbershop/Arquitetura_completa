package ifsp.edu.projeto.cortaai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Mapeamento global para cobrir a API, o Swagger e tudo o mais.
                .allowedOrigins(
                        "https://www.cortaai.oneaction.space", // Domínio principal do seu front-end
                        "https://api.cortaai.oneaction.space"  // Domínio da própria API (para o Swagger)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}