package ifsp.edu.projeto.cortaai.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // Opcional agora, mas bom já ter

@SpringBootApplication
@EnableDiscoveryClient // Habilita o cliente Eureka (para o futuro)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

}