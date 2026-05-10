package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "payment-service", path = "/api/payments")
public interface PaymentServiceClient {

    /**
     * Retorna performance financeira dos barbeiros da barbearia.
     * Reutiliza o endpoint de analytics já existente.
     */
    @GetMapping("/analytics/barber-performance")
    List<Map<String, Object>> getBarberPerformance(@RequestParam("barbershopId") UUID barbershopId);
}
