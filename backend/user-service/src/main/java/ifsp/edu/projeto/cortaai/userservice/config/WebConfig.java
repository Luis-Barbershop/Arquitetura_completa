package ifsp.edu.projeto.cortaai.userservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.MediaType; 
import org.springframework.http.converter.HttpMessageConverter; 
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "https://www.cortaai.oneaction.space", // Versão com www
                        "https://cortaai.oneaction.space",   // ADICIONADO: Versão sem www
                        "https://api.cortaai.oneaction.space",  // Para o Swagger
                        "http://localhost:5173", //local    
                        "http://localhost:3000" //Rota alternativa
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Procura o conversor padrão de JSON e adiciona o suporte a 'octet-stream'
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter) converter).setSupportedMediaTypes(Arrays.asList(
                        MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_OCTET_STREAM // <--- O segredo está aqui
                ));
            }
        }
    }
}


