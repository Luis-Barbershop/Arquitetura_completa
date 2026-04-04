package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.InventoryFinancialSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/internal/products/barbershops/{barbershopId}/financial-summary")
    InventoryFinancialSummaryDTO getFinancialSummary(
            @PathVariable("barbershopId") UUID barbershopId,
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to
    );
}

