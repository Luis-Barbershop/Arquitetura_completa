package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.CommissionRuleInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "barbershop-service", path = "/api/internal/barbershops")
public interface BarbershopServiceClient {

    @GetMapping("/{shopId}/barbers/{barberId}/commissions")
    List<CommissionRuleInfoDTO> getBarberCommissions(
            @PathVariable("shopId") UUID shopId,
            @PathVariable("barberId") UUID barberId);
}
