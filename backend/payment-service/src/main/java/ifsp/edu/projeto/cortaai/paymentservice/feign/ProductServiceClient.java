package ifsp.edu.projeto.cortaai.paymentservice.feign;

import ifsp.edu.projeto.cortaai.paymentservice.dto.InventoryFinancialSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Feign Client para comunicação com o product-service.
 *
 * ATENÇÃO: parâmetros de data são String ISO-8601 (yyyy-MM-dd).
 * @DateTimeFormat NÃO controla serialização no Feign — o ConversionService do locale pt_BR
 * produziria "dd/MM/yyyy" e quebraria o endpoint do product-service.
 * A conversão deve ser feita explicitamente no call site com DateTimeFormatter.ISO_LOCAL_DATE.
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/internal/products/barbershops/{barbershopId}/financial-summary")
    InventoryFinancialSummaryDTO getFinancialSummary(
            @PathVariable("barbershopId") UUID barbershopId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    );
}

