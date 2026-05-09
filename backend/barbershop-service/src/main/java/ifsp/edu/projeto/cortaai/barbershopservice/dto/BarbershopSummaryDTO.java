package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.UUID;

public record BarbershopSummaryDTO(
        UUID id,
        String name,
        String address,
        String logoUrl,
        Double averageRating,
        Long reviewsCount,
        Double latitude,
        Double longitude,
        Double distanceKm
) {}
