package ifsp.edu.projeto.cortaai.schedule.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "ifsp.edu.projeto.cortaai.schedule.client")
public class FeignConfig {
}
