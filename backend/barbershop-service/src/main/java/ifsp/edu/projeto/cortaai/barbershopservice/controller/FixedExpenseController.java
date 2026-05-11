package ifsp.edu.projeto.cortaai.barbershopservice.controller;

import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseRequestDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.dto.FixedExpenseResponseDTO;
import ifsp.edu.projeto.cortaai.barbershopservice.service.FixedExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/barbershops/my-shop/fixed-expenses")
@RequiredArgsConstructor
@Tag(name = "Gastos Fixos", description = "Gestão de despesas mensais fixas da barbearia")
public class FixedExpenseController {

    private final FixedExpenseService fixedExpenseService;

    @Operation(summary = "Lista gastos fixos", description = "Retorna os gastos fixos do mês/ano informado, ou do ano inteiro se month não for fornecido.")
    @GetMapping
    public ResponseEntity<List<FixedExpenseResponseDTO>> list(
            @Parameter(hidden = true) Principal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(fixedExpenseService.list(principal.getName(), month, year));
    }

    @Operation(summary = "Cadastra gasto fixo")
    @PostMapping
    public ResponseEntity<FixedExpenseResponseDTO> create(
            @Parameter(hidden = true) Principal principal,
            @RequestBody @Valid FixedExpenseRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(fixedExpenseService.create(principal.getName(), dto));
    }

    @Operation(summary = "Remove gasto fixo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) Principal principal,
            @PathVariable UUID id) {
        fixedExpenseService.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
