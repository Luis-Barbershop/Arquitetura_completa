package ifsp.edu.projeto.cortaai.scheduleservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "product-service", path = "/api/products")
public interface ProductServiceClient {

    /**
     * Retorna alertas de estoque para a barbearia.
     * Reutiliza o endpoint de analytics já existente.
     */
    @GetMapping("/analytics/stock-health")
    List<Map<String, Object>> getStockHealth(@RequestParam("barbershopId") UUID barbershopId);
}
