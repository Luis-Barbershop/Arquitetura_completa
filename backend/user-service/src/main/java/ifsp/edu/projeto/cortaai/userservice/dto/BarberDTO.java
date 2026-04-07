package ifsp.edu.projeto.cortaai.userservice.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

public record BarberDTO(
        UUID id,
        String name,
        String email,
        String tell,
        String documentCPF,
        LocalDate birthDate,
        Boolean isOwner,
        Boolean actAsBarber,
        UUID barbershopId,
        LocalTime workStartTime,
        LocalTime workEndTime,
        String imageUrl,
        Set<UUID> assignedActivityIds
) {}


