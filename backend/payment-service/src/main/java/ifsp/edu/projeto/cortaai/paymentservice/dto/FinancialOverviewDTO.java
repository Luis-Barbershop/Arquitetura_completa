package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Visao financeira da barbearia para dashboard interno.
 */
public record FinancialOverviewDTO(
        UUID barbershopId,
        String currency,
        BigDecimal serviceRevenue,
        BigDecimal walkInRevenue,
        BigDecimal totalServiceRevenue,
        BigDecimal productExpenses,
        BigDecimal inventoryAssetValue,
        BigDecimal operationalResult,
        BigDecimal operationalResultWithWalkIn,
        int transactionsCount,
        int walkInAppointmentsCount,
        int approvedCount,
        int pendingCount,
        int cancelledCount
) {}

