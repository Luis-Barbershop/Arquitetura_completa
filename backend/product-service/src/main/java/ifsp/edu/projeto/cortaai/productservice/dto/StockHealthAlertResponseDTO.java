package ifsp.edu.projeto.cortaai.productservice.dto;

public record StockHealthAlertResponseDTO(
        String productId,
        String productName,
        String category,
        Integer currentStock,
        Integer predictedMinimum,
        boolean requiresRestock
) {}
