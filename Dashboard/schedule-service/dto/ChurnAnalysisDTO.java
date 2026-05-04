package ifsp.edu.projeto.cortaai.scheduleservice.dto.analytics;

public record ChurnAnalysisDTO(
    String barberId,
    String barberName,
    Double averageRating,
    Long abandonedCustomersCount
) {}