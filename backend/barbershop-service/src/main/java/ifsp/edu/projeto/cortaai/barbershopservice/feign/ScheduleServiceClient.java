package ifsp.edu.projeto.cortaai.barbershopservice.feign;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.AppointmentSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "schedule-service", path = "/api/internal/appointments")
public interface ScheduleServiceClient {

    @GetMapping("/by-barber/{barberId}/future")
    List<AppointmentSummaryDTO> getFutureAppointmentsByBarber(@PathVariable("barberId") UUID barberId);
}
