package ifsp.edu.projeto.cortaai.userservice.dto;

import java.time.LocalTime;
import java.util.UUID;

public record BarberDTO(
        UUID id,
        String name,
        String email,
        String tell,
        String documentCPF,
        Boolean isOwner,
        UUID barbershopId,
        LocalTime workStartTime,
        LocalTime workEndTime,
        String imageUrl
) {}


