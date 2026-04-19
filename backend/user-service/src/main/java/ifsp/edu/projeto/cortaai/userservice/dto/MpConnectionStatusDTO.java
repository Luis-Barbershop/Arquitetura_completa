package ifsp.edu.projeto.cortaai.userservice.dto;

/**
 * DTO interno para representar o estado de vínculo do barbeiro com Mercado Pago.
 */
public record MpConnectionStatusDTO(
        boolean linked,
        String mpUserIdMasked,
        boolean hasPublicKey
) {
}
