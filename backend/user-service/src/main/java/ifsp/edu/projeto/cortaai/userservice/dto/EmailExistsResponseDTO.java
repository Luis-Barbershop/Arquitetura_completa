package ifsp.edu.projeto.cortaai.userservice.dto;

/**
 * Resposta do endpoint GET /api/auth/email/exists?email=...
 *
 * @param exists    true se o e-mail já está cadastrado em qualquer perfil.
 * @param userType  "CUSTOMER", "BARBER" ou null quando exists=false.
 */
public record EmailExistsResponseDTO(boolean exists, String userType) {}
