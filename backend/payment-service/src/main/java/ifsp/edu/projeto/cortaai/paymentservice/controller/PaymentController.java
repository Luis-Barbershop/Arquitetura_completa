package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.CreatePaymentDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.MpConnectionStatusDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou header X-User-Id ausente",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno ou falha no Mercado Pago",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<TransactionDTO> createPayment(
            @Parameter(description = "Dados para criação do pagamento (ex: ID do agendamento, método de pagamento)") @Valid @RequestBody CreatePaymentDTO dto,
            @Parameter(description = "Firebase UID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        TransactionDTO transaction = paymentService.createPaymentByFirebaseUid(dto.appointmentId(), firebaseUid, dto.paymentMethod());
        return ResponseEntity.ok(transaction);
    }

    /**
     * Busca uma transação por ID.
     */
    @Operation(summary = "Buscar transação por ID", description = "Busca os detalhes de uma transação de pagamento específica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transação encontrada"),
            @ApiResponse(responseCode = "404", description = "Transação não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
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
            @Parameter(description = "Firebase UID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        return ResponseEntity.ok(paymentService.getMyPaymentsByFirebaseUid(firebaseUid));
    }

        @Operation(summary = "Status da conexão Mercado Pago", description = "Retorna o status de vínculo da conta Mercado Pago do owner autenticado.")
        @GetMapping("/mp-status")
        public ResponseEntity<MpConnectionStatusDTO> getMpConnectionStatus(
                        @Parameter(description = "Firebase UID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
                return ResponseEntity.ok(paymentService.getMpConnectionStatusByFirebaseUid(firebaseUid));
        }

        @Operation(summary = "Desvincular conta Mercado Pago", description = "Remove as credenciais OAuth de Mercado Pago do owner autenticado.")
        @PutMapping("/mp-disconnect")
        public ResponseEntity<Void> disconnectMpConnection(
                        @Parameter(description = "Firebase UID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
                paymentService.disconnectMpByFirebaseUid(firebaseUid);
                return ResponseEntity.noContent().build();
        }

    @Operation(summary = "Resumo financeiro da barbearia", description = "Retorna receita de serviços, gastos com produtos (estoque interno), valor atual dos bens em estoque e resultado operacional no período.")
    @GetMapping("/my-shop/overview")
    public ResponseEntity<FinancialOverviewDTO> getMyShopOverview(
            @Parameter(description = "Firebase UID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @Parameter(description = "UUID da barbearia") @RequestParam UUID barbershopId,
            @Parameter(description = "Data inicial (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(paymentService.getBarbershopOverviewByFirebaseUid(firebaseUid, barbershopId, from, to));
    }

    @Operation(summary = "Serie financeira da barbearia", description = "Retorna série de receita de serviços aprovados por período para uso em gráficos internos.")
    @GetMapping("/my-shop/series")
    public ResponseEntity<FinancialSeriesDTO> getMyShopSeries(
            @Parameter(description = "Firebase UID do usuário autenticado (injetado via Gateway)", hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @Parameter(description = "UUID da barbearia") @RequestParam UUID barbershopId,
            @Parameter(description = "Data inicial (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final (YYYY-MM-DD)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Agrupamento: DAY ou WEEK") @RequestParam(required = false, defaultValue = "DAY") String groupBy) {
        return ResponseEntity.ok(paymentService.getBarbershopSeriesByFirebaseUid(firebaseUid, barbershopId, from, to, groupBy));
    }

    @Operation(summary = "Desempenho financeiro por barbeiro", description = "Retorna ranking de barbeiros com receita gerada e participação percentual. Apenas o owner pode acessar.")
    @GetMapping("/my-shop/barber-performance")
    public ResponseEntity<List<BarberFinancialPerformanceResponseDTO>> getBarberPerformance(
            @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(paymentService.getBarberFinancialPerformance(firebaseUid, barbershopId));
    }
}