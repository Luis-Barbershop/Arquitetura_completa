package ifsp.edu.projeto.cortaai.productservice.dto;

import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        String name,
        UUID barbershopId
) {}
