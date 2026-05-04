package ifsp.edu.projeto.cortaai.barbershopservice.security.crypto;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataCryptoConfiguration {

    @Value("${app.security.crypto.key:}")
    private String dataCryptoKey;

    @PostConstruct
    void configure() {
        DataCrypto.configure(dataCryptoKey);
    }
}
