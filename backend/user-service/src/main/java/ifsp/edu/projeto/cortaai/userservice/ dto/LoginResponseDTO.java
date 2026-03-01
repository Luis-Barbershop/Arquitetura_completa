package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.UUID;

public record LoginResponseDTO(
        String token,
        String name,
        String role,
        UUID id
) {}