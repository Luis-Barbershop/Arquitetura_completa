package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/payments/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class PaymentAnalyticsController {

    private final PaymentService paymentService;

    @GetMapping("/barber-performance")
    public ResponseEntity<List<BarberFinancialPerformanceResponseDTO>> getBarberPerformance(
            @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(paymentService.getBarberFinancialPerformance(firebaseUid, barbershopId));
    }
}
