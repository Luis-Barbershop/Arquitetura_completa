package ifsp.edu.projeto.cortaai.userservice.dto;

/**
 * Credenciais Mercado Pago recebidas pelo payment-service após o OAuth e salvas no barbeiro.
 */
public record SaveMpCredentialsDTO(
        String mpAccessToken,
        String mpRefreshToken,
        String mpUserId,
        String mpPublicKey
) {}
