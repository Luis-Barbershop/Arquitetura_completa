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
        BigDecimal productExpenses,
        BigDecimal inventoryAssetValue,
        BigDecimal operationalResult,
        int transactionsCount,
        int approvedCount,
        int pendingCount,
        int cancelledCount
) {}

