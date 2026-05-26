package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", contextId = "userAnalyticsClient", path = "/api/users/analytics")
public interface UserAnalyticsClient {

    @GetMapping("/customer-acquisition")
    List<Map<String, Object>> getCustomerAcquisition();
}
