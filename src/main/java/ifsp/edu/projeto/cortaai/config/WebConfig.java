package ifsp.edu.projeto.cortaai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a configuração a TODAS as rotas
                // Adicione a URL do seu front-end aqui.
                // Se tiver uma para desenvolvimento local, adicione também.
                .allowedOrigins("https://www.cortaai.oneaction.space", "http://localhost:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true); // Permite o envio de credenciais (importante para sessões/tokens)
    }
}