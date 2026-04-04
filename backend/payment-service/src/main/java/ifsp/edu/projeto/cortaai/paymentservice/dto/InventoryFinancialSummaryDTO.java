package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryFinancialSummaryDTO(
        UUID barbershopId,
        BigDecimal productExpenses,
        BigDecimal inventoryAssetValue
) {}

