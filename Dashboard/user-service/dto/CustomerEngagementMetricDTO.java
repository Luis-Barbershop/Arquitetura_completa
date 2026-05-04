package ifsp.edu.projeto.cortaai.userservice.dto.analytics;

public record CustomerEngagementMetricsDTO(
    String referenceMonth,
    Long newCustomers,
    Long returningCustomers,
    Double retentionRatePercentage
) {}