package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.UUID;

public record RemoveTeamMemberRequestDTO(
        String action,
        UUID redistributeToId
) {}
