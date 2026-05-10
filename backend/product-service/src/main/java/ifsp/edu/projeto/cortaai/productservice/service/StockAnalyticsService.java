package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockAnalyticsService {

    private final ProductRepository productRepository;

    public List<StockHealthAlertResponseDTO> getStockHealthAlert(String barbershopId) {
        log.info("Consultando saúde do estoque para barbearia: {}", barbershopId);
        return productRepository.findStockHealthByBarbershopId(UUID.fromString(barbershopId))
                .stream()
                .map(p -> new StockHealthAlertResponseDTO(
                        p.getId().toString(),
                        p.getName(),
                        p.getCategory() != null ? p.getCategory().name() : null,
                        p.getStockQuantity(),
                        p.getMinStockQuantity(),
                        p.getStockQuantity() <= p.getMinStockQuantity()
                ))
                .toList();
    }
}
