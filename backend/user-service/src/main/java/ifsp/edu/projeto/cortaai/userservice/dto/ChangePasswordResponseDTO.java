package ifsp.edu.projeto.cortaai.userservice.dto;

/**
 * Retornado após troca de senha bem-sucedida.
 * O Firebase invalida o token antigo e emite um novo par idToken/refreshToken.
 * O cliente deve substituir o token em sessão — sem precisar fazer novo login.
 */
public record ChangePasswordResponseDTO(
        String idToken,
        String refreshToken
) {}
