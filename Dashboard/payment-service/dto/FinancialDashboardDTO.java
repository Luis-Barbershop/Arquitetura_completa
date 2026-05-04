package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinancialDashboardDTO(
    BigDecimal currentMonthRevenue,
    BigDecimal currentMonthProfit,
    List<BarberPerformanceDTO> barberPerformance
) {}