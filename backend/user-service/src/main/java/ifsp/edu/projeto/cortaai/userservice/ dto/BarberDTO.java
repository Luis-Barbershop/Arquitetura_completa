package ifsp.edu.projeto.cortaai.userservice.dto;

import java.util.UUID;

// Use record ou class, dependendo de como estava. Se for record:
public record BarberDTO(
        UUID id,             // <--- Mudou de Long para UUID
        String name,
        String email,
        String tell,
        String documentCPF,
        Boolean isOwner,
        UUID barbershopId    // <--- Mudou de Long para UUID (se houver esse campo)
) {}