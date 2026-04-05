package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.UUID;

public record BarberPublicDTO(
        UUID id,
        String name,
        String imageUrl
) {}

