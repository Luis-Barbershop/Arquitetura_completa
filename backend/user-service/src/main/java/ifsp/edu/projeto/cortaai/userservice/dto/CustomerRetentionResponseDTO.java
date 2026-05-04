package ifsp.edu.projeto.cortaai.userservice.dto;

public record CustomerRetentionResponseDTO(
        String referenceMonth,
        Long returningCustomers
) {}
