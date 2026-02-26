package ifsp.edu.projeto.cortaai.schedule.client;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BarbershopServiceClientFallback implements BarbershopServiceClient {

    @Override
    public BarbershopDTO getBarbershopById(UUID id) {
        return new BarbershopDTO(id, "Unknown", "00000000000000", null);
    }
}
