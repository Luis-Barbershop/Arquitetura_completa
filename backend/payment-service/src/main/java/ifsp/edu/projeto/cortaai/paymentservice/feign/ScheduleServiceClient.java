package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Feign Client para comunicação com o schedule-service.
 * Usa endpoints internos (não expostos no Gateway).
 *
 * ATENÇÃO: parâmetros de data são String ISO-8601 (yyyy-MM-ddTHH:mm:ss).
 * @DateTimeFormat NÃO controla serialização no Feign — o ConversionService do locale pt_BR
 * produziria "dd/MM/yyyy HH:mm" e quebraria o endpoint do schedule-service.
 * A conversão para String deve ser feita explicitamente no call site com DateTimeFormatter.ISO_LOCAL_DATE_TIME.
 */
@FeignClient(name = "schedule-service")
public interface ScheduleServiceClient {

    @GetMapping("/api/internal/appointments/{id}")
    AppointmentInfoDTO getAppointmentById(@PathVariable("id") UUID id);

    @GetMapping("/api/internal/appointments/barbershop/{barbershopId}/period")
    List<AppointmentInfoDTO> getBarbershopAppointmentsByPeriod(
            @PathVariable("barbershopId") UUID barbershopId,
            @RequestParam("from") String from,
            @RequestParam("to") String to);

    @PutMapping("/api/internal/appointments/{id}/payment-status")
    void updatePaymentStatus(@PathVariable("id") UUID id, @RequestParam("status") String status);
}
