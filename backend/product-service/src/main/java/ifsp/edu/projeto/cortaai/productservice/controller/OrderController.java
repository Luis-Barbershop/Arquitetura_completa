package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateOrderDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.OrderDTO;
import ifsp.edu.projeto.cortaai.productservice.model.OrderStatus;
import ifsp.edu.projeto.cortaai.productservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller REST para pedidos.
 * Endpoints públicos (expostos via Gateway).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "NOVO: Endpoints para criação e gestão de pedidos de e-commerce")
public class OrderController {

    private final OrderService orderService;

    /**
     * Cria um novo pedido.
     */
    @Operation(summary = "Criar pedido", description = "Registra um novo pedido de produtos feito por um cliente autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso")
    })
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Parameter(description = "Itens e detalhes do pedido") @Valid @RequestBody CreateOrderDTO dto,
            @Parameter(description = "ID do cliente (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        OrderDTO order = orderService.createOrder(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Lista pedidos do usuário logado.
     */
    @Operation(summary = "Listar meus pedidos", description = "Retorna o histórico de todos os pedidos realizados pelo cliente logado.")
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders(
            @Parameter(description = "ID do cliente (injetado via Gateway)", hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    /**
     * Lista pedidos de uma barbearia (para o dono/barbeiro).
     */
    @Operation(summary = "Listar pedidos da barbearia", description = "Retorna todos os pedidos de produtos feitos para uma barbearia específica (usado no painel de gestão do dono).")
    @GetMapping("/shop-orders")
    public ResponseEntity<List<OrderDTO>> getShopOrders(
            @Parameter(description = "UUID da barbearia") @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(orderService.getShopOrders(barbershopId));
    }

    /**
     * Atualiza o status de um pedido.
     */
    @Operation(summary = "Atualizar status do pedido", description = "Muda o estado atual do pedido (ex: PENDING → PAID → PREPARING → READY → DELIVERED).")
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @Parameter(description = "UUID do pedido") @PathVariable UUID id,
            @Parameter(description = "Novo status do pedido") @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}