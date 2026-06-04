package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.Map;

/**
 * Progresso de onboarding agrupado por papel (customer, barber, owner).
 */
public record OnboardingRoleProgressDTO(
        Map<String, OnboardingPageProgressDTO> completedPages
) {}
