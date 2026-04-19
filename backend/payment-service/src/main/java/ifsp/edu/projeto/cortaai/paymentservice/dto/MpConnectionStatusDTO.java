package ifsp.edu.projeto.cortaai.paymentservice.dto;

/**
 * Estado de vínculo da conta Mercado Pago do barbeiro autenticado.
 */
public record MpConnectionStatusDTO(
        boolean linked,
        String mpUserIdMasked,
        boolean hasPublicKey
) {
}
