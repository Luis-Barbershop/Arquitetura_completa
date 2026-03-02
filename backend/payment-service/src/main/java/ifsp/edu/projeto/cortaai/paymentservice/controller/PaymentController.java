package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.dto.CreatePaymentDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para pagamentos.
 * Endpoints públicos (expostos via Gateway).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "NOVO: Endpoints para criação e consulta de pagamentos de agendamentos via Mercado Pago")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Cria um pagamento para um agendamento.
     * Retorna a URL de checkout do Mercado Pago.
     */
    @Operation(summary = "Criar pagamento", description = "Gera uma nova intenção de pagamento para um agendamento no Mercado Pago. Retorna a transação contendo a URL de checkout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou agendamento não encontrado")
    })
    @PostMapping("/create")
    public ResponseEntity<TransactionDTO> createPayment(
            @Parameter(description = "Dados para criação do pagamento (ex: ID do agendamento)") @Valid @RequestBody CreatePaymentDTO dto,
            @Parameter(description = "ID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        TransactionDTO transaction = paymentService.createPayment(dto.appointmentId(), userId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Busca uma transação por ID.
     */
    @Operation(summary = "Buscar transação por ID", description = "Busca os detalhes de uma transação de pagamento específica.")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getById(
            @Parameter(description = "UUID da transação") @PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    /**
     * Lista todos os pagamentos do usuário logado.
     */
    @Operation(summary = "Listar meus pagamentos", description = "Lista todas as transações de pagamento vinculadas ao usuário autenticado (identificado pelo header X-User-Id).")
    @GetMapping("/my-payments")
    public ResponseEntity<List<TransactionDTO>> getMyPayments(
            @Parameter(description = "ID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }
}