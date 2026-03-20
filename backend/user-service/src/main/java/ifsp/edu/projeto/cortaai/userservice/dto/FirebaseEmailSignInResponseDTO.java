package ifsp.edu.projeto.cortaai.userservice.dto;

public record FirebaseEmailSignInResponseDTO(
        String idToken,
        String refreshToken,
        String expiresIn,
        String localId,
        String email,
        Boolean registered
) {
}

