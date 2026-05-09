package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.repository.analytics.VStockHealthAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StockAnalyticsService {

    private final VStockHealthAlertRepository vStockHealthAlertRepository;

    public List<StockHealthAlertResponseDTO> getStockHealthAlert(String barbershopId) {
        log.info("Consultando saúde do estoque para barbearia: {}", barbershopId);
        return vStockHealthAlertRepository.findByBarbershopId(barbershopId)
                .stream()
                .map(p -> new StockHealthAlertResponseDTO(
                        p.getProductId(),
                        p.getProductName(),
                        p.getCategory(),
                        p.getCurrentStock(),
                        p.getPredictedMinimum(),
                        p.getRequiresRestock() != null && p.getRequiresRestock() == 1
                ))
                .toList();
    }
}
