package ifsp.edu.projeto.cortaai.notificationservice.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class ScheduleServiceClientFallback implements ScheduleServiceClient {

    @Override
    public AppointmentInfoDTO getAppointmentById(UUID id) {
        log.warn("schedule-service indisponível ao buscar appointment id={}", id);
        return null;
    }
}
