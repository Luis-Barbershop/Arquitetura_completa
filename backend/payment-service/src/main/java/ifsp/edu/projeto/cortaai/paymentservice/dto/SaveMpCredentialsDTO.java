package ifsp.edu.projeto.cortaai.paymentservice.dto;

/**
 * Dados do OAuth do Mercado Pago a serem salvos no user-service.
 */
public record SaveMpCredentialsDTO(
        String mpAccessToken,
        String mpRefreshToken,
        String mpUserId,
        String mpPublicKey
) {}
