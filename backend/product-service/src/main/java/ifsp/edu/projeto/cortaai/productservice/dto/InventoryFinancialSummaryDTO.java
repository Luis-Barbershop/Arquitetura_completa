package ifsp.edu.projeto.cortaai.productservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resumo financeiro interno do estoque da barbearia.
 * productExpenses representa compras/entradas no periodo.
 * inventoryAssetValue representa o valor atual do estoque (custo estimado).
 */
public record InventoryFinancialSummaryDTO(
        UUID barbershopId,
        BigDecimal productExpenses,
        BigDecimal inventoryAssetValue
) {}

