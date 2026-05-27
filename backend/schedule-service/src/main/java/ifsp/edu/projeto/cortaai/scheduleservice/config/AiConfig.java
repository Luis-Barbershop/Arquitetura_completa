package ifsp.edu.projeto.cortaai.scheduleservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiConfig {

    /** RestTemplate usado pelo AiChatServiceImpl para chamadas às APIs OpenRouter e Cohere. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
