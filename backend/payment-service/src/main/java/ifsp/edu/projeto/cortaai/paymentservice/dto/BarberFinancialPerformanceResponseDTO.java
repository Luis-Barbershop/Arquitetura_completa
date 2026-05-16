package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;

public record BarberFinancialPerformanceResponseDTO(
        String barberId,
        String barberName,
        Long totalAppointments,
        BigDecimal generatedRevenue,
        BigDecimal contributionPercentage,
        BigDecimal barberCommission,
        BigDecimal barbershopCommission,
        BigDecimal averageTicket
) {
    public BarberFinancialPerformanceResponseDTO(
            String barberId,
            String barberName,
            Long totalAppointments,
            BigDecimal generatedRevenue,
            BigDecimal contributionPercentage) {
        this(
                barberId,
                barberName,
                totalAppointments,
                generatedRevenue,
                contributionPercentage,
                BigDecimal.ZERO,
                generatedRevenue,
                BigDecimal.ZERO
        );
    }
}
