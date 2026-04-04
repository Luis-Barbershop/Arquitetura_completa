package ifsp.edu.projeto.cortaai.productservice.dto;

import java.util.List;

public record InventoryPageDTO(
        List<InventoryProductItemDTO> items,
        int page,
        int size,
        long total,
        int totalPages
) {}

