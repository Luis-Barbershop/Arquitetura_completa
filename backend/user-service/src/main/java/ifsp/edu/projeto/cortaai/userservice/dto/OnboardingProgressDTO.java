package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.Map;

/**
 * Snapshot de progresso de onboarding por conta e por papel.
 */
public record OnboardingProgressDTO(
        Integer version,
        Map<String, OnboardingRoleProgressDTO> progressByRole
) {}
