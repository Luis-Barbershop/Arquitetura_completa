package ifsp.edu.projeto.cortaai.paymentservice.dto.analytics;

import java.math.BigDecimal;

public record BarberPerformanceDTO(
    String barberId,
    String barberName,
    BigDecimal generatedRevenue,
    Integer totalAppointments
) {}