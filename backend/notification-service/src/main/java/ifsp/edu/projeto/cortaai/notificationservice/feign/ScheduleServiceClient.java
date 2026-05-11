package ifsp.edu.projeto.cortaai.notificationservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Feign Client para o schedule-service — busca dados de agendamento para notificações.
 * Usa o endpoint interno do InternalAppointmentController.
 */
@FeignClient(name = "schedule-service", path = "/api/internal/appointments",
        fallback = ScheduleServiceClientFallback.class)
public interface ScheduleServiceClient {

    @GetMapping("/{id}")
    AppointmentInfoDTO getAppointmentById(@PathVariable("id") UUID id);
}
