package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.Map;

public record FirebaseTokenDebugResponseDTO(
        String uid,
        String email,
        String name,
        String issuer,
        String audience,
        String issuedAt,
        String expiresAt,
        Map<String, Object> claims
) {
}

