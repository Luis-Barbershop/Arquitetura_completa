package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/payments/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PaymentAnalyticsController {

    private final PaymentAnalyticsService paymentAnalyticsService;

    @GetMapping("/barber-performance")
    public ResponseEntity<List<BarberFinancialPerformanceResponseDTO>> getBarberPerformance() {
        return ResponseEntity.ok(paymentAnalyticsService.getBarberFinancialPerformance());
    }
}
