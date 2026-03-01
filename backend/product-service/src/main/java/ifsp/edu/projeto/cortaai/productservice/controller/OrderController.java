package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateOrderDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.OrderDTO;
import ifsp.edu.projeto.cortaai.productservice.model.OrderStatus;
import ifsp.edu.projeto.cortaai.productservice.service.OrderService;
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
public class OrderController {

    private final OrderService orderService;

    /**
     * Cria um novo pedido.
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody CreateOrderDTO dto,
            @RequestHeader("X-User-Id") UUID userId) {
        OrderDTO order = orderService.createOrder(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Lista pedidos do usuário logado.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    /**
     * Lista pedidos de uma barbearia (para o dono/barbeiro).
     */
    @GetMapping("/shop-orders")
    public ResponseEntity<List<OrderDTO>> getShopOrders(
            @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(orderService.getShopOrders(barbershopId));
    }

    /**
     * Atualiza o status de um pedido.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, status));
    }
}
