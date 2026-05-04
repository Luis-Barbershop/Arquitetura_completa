package ifsp.edu.projeto.cortaai.productservice.dto.analytics;

import java.math.BigDecimal;

public record StockPredictabilityDTO(
    String productId,
    String productName,
    Integer currentStock,
    BigDecimal predictedConsumption,
    Boolean requiresRestock
) {}