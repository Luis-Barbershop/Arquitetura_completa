package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.AppointmentInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Feign Client para comunicação com o schedule-service.
 * Usa endpoints internos (não expostos no Gateway).
 */
@FeignClient(name = "schedule-service")
public interface ScheduleServiceClient {

    @GetMapping("/api/internal/appointments/{id}")
    AppointmentInfoDTO getAppointmentById(@PathVariable("id") UUID id);

    @GetMapping("/api/internal/appointments/barbershop/{barbershopId}/period")
    List<AppointmentInfoDTO> getBarbershopAppointmentsByPeriod(
            @PathVariable("barbershopId") UUID barbershopId,
            @RequestParam("from") LocalDateTime from,
            @RequestParam("to") LocalDateTime to);

    @PutMapping("/api/internal/appointments/{id}/payment-status")
    void updatePaymentStatus(@PathVariable("id") UUID id, @RequestParam("status") String status);
}
