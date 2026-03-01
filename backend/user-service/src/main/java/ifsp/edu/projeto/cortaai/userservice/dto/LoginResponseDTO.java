package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public record LoginResponseDTO(
        String token,
        Object userData,
        String name,
        String role,
        UUID id
) {
    // Constructor for backward compatibility with BarberServiceImpl
    public LoginResponseDTO(String token, String name, String role, UUID id) {
        this(token, null, name, role, id);
    }
}

