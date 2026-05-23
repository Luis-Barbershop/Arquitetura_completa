package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
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

    @GetMapping("/my-shop/overview")
    Map<String, Object> getMyShopOverview(
            @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam("barbershopId") UUID barbershopId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/my-shop/barber-summary")
    Map<String, Object> getMyBarberSummary(
            @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam("barbershopId") UUID barbershopId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/my-shop/barber-performance")
    List<Map<String, Object>> getMyShopBarberPerformance(
            @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam("barbershopId") UUID barbershopId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);
}
