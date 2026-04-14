package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@FeignClient(name = "user-service", path = "/api/internal/users")
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserInfoDTO getUserById(@PathVariable("id") UUID id);

    @GetMapping("/by-email/{email}")
    UserInfoDTO getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/by-firebase-uid/{uid}")
    UserInfoDTO getUserByFirebaseUid(@PathVariable("uid") String uid);

    /**
     * Busca a agenda semanal (blocos de horário por dia) de um barbeiro.
     * Rota pública no BarberController: GET /api/barbers/{id}/work-schedule
     */
    @GetMapping("/barbers/{id}/work-schedule")
    List<DayScheduleDTO> getBarberWorkSchedule(@PathVariable("id") UUID barberId);

    @GetMapping("/barbers/{id}/activities")
    Set<UUID> getBarberAssignedActivities(@PathVariable("id") UUID barberId);
}

