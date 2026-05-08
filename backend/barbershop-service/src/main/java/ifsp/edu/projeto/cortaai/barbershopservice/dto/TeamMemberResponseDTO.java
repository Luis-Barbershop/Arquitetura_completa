package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TeamMemberResponseDTO(
        UUID barberId,
        String name,
        String imageUrl,
        String email,
        Boolean isOwner,
        LocalTime workStartTime,
        LocalTime workEndTime,
        List<CommissionRuleDTO> commissions
) {}
