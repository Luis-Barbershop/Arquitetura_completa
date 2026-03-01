package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.UUID;

/**
 * DTO inter-serviço — consumido pelo schedule-service via Feign.
 */
public record BarbershopInfoDTO(
        UUID id,
        UUID ownerId,
        String name,
        String cnpj,
        String address
) {}

