package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Endpoints internos para consumo entre microservicos.
 */
@RestController
@RequestMapping("/api/internal/products")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/barbershops/{barbershopId}/financial-summary")
    public ResponseEntity<InventoryFinancialSummaryDTO> getFinancialSummary(
            @PathVariable UUID barbershopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(productService.getFinancialSummary(barbershopId, from, to));
    }
}

