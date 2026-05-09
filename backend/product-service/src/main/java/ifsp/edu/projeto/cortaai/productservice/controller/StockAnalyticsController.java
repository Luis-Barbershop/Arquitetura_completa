package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.service.StockAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/products/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class StockAnalyticsController {

    private final StockAnalyticsService stockAnalyticsService;

    @GetMapping("/stock-health")
    public ResponseEntity<List<StockHealthAlertResponseDTO>> getStockHealth(
            @RequestParam String barbershopId) {
        return ResponseEntity.ok(stockAnalyticsService.getStockHealthAlert(barbershopId));
    }
}
