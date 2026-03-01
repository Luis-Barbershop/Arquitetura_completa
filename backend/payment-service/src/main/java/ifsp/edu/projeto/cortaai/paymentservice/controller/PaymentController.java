package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.dto.CreatePaymentDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
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
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Cria um pagamento para um agendamento.
     * Retorna a URL de checkout do Mercado Pago.
     */
    @PostMapping("/create")
    public ResponseEntity<TransactionDTO> createPayment(
            @Valid @RequestBody CreatePaymentDTO dto,
            @RequestHeader("X-User-Id") UUID userId) {
        TransactionDTO transaction = paymentService.createPayment(dto.appointmentId(), userId);
        return ResponseEntity.ok(transaction);
    }

    /**
     * Busca uma transação por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getById(id));
    }

    /**
     * Lista todos os pagamentos do usuário logado.
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<TransactionDTO>> getMyPayments(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }
}
