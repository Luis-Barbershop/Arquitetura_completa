package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.UUID;

/**
 * Resposta retornada pelo endpoint {@code POST /api/auth/verify}.
 *
 * <p>{@code profileComplete} indica se o usuário ainda precisa preencher
 * dados extras (CPF, telefone, horários de trabalho), útil no fluxo de
 * login social onde o Firebase não fornece esses dados.
 */
public record AuthResponseDTO(
        UUID id,
        String name,
        String email,
        String phone,
        String photoUrl,
        String userType,       // CUSTOMER | BARBER
        String authProvider,   // EMAIL | GOOGLE | FACEBOOK | APPLE | GITHUB | TWITTER
        boolean profileComplete,
        String role,           // ROLE_CUSTOMER | ROLE_BARBER | ROLE_OWNER
        boolean emailVerified,
        boolean verificationRequired,
        // Campos extras para barbeiros (null para customers)
        UUID barbershopId,
        Boolean isOwner
) {}
