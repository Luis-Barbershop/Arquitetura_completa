package ifsp.edu.projeto.cortaai.userservice.dto;

/**
 * Resposta do endpoint de cadastro de teste.
 * Inclui o idToken do Firebase (para uso no Swagger) e o perfil já completo.
 */
public record FirebaseEmailRegisterResponseDTO(
        /** Firebase ID Token — use em endpoints que exigem Authorization: Bearer */
        String idToken,
        String refreshToken,
        String expiresIn,
        String localId,
        /** Perfil completo já criado no banco de dados. */
        AuthResponseDTO profile
) {}
