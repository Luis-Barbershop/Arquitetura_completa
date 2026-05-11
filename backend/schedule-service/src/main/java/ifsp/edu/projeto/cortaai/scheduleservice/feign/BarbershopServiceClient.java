package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CommissionRuleInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "barbershop-service", path = "/api/internal/barbershops")
public interface BarbershopServiceClient {

    @GetMapping("/{id}")
    BarbershopInfoDTO getBarbershopById(@PathVariable("id") UUID id);

    @GetMapping("/{shopId}/activities")
    List<ActivityInfoDTO> getActivitiesByIds(@PathVariable("shopId") UUID shopId,
                                             @RequestParam("ids") List<UUID> ids);

    @GetMapping("/{shopId}/activities/all")
    List<ActivityInfoDTO> getAllActivities(@PathVariable("shopId") UUID shopId);

    @GetMapping("/{shopId}/barbers/{barberId}/commissions")
    List<CommissionRuleInfoDTO> getBarberCommissions(@PathVariable("shopId") UUID shopId,
                                                     @PathVariable("barberId") UUID barberId);
}

