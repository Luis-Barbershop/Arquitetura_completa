package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;

public record BarberFinancialPerformanceResponseDTO(
        String barberId,
        String barberName,
        Long totalAppointments,
        BigDecimal generatedRevenue,
        BigDecimal contributionPercentage
) {}
