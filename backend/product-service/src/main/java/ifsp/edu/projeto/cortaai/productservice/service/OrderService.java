package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.*;
import ifsp.edu.projeto.cortaai.productservice.mapper.OrderMapper;
import ifsp.edu.projeto.cortaai.productservice.model.*;
import ifsp.edu.projeto.cortaai.productservice.repository.OrderRepository;
import ifsp.edu.projeto.cortaai.productservice.repository.ProductRepository;
import ifsp.edu.projeto.cortaai.productservice.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final OrderMapper orderMapper;

    /**
     * Cria um pedido:
     * 1. Valida estoque de cada produto
     * 2. Cria Order + OrderItems com snapshots
     * 3. Baixa estoque (StockMovement OUT)
     */
    @Transactional
    public OrderDTO createOrder(CreateOrderDTO dto, UUID customerId) {
        Order order = Order.builder()
                .customerId(customerId)
                .barbershopId(dto.barbershopId())
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        for (OrderItemRequestDTO itemReq : dto.items()) {
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + itemReq.productId()));

            if (!product.isActive()) {
                throw new RuntimeException("Produto indisponível: " + product.getName());
            }

            if (product.getStockQuantity() < itemReq.quantity()) {
                throw new RuntimeException(
                        String.format("Estoque insuficiente para '%s'. Disponível: %d, Solicitado: %d",
                                product.getName(), product.getStockQuantity(), itemReq.quantity())
                );
            }

            // Criar item com snapshot
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(itemReq.quantity())
                    .build();
            order.getItems().add(item);

            // Baixar estoque
            product.setStockQuantity(product.getStockQuantity() - itemReq.quantity());
            productRepository.save(product);

            // Registrar movimentação de saída
            StockMovement movement = StockMovement.builder()
                    .productId(product.getId())
                    .type(MovementType.OUT)
                    .quantity(itemReq.quantity())
                    .reason("Venda - Pedido")
                    .build();
            stockMovementRepository.save(movement);
        }

        order.calculateTotal();
        Order saved = orderRepository.save(order);

        // Atualizar referência do orderId nos movimentos
        stockMovementRepository.findAll().stream()
                .filter(m -> m.getOrderId() == null && "Venda - Pedido".equals(m.getReason()))
                .forEach(m -> {
                    m.setOrderId(saved.getId());
                    stockMovementRepository.save(m);
                });

        log.info("Pedido criado: id={}, total={}", saved.getId(), saved.getTotalPrice());
        return orderMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getMyOrders(UUID customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getShopOrders(UUID barbershopId) {
        return orderRepository.findByBarbershopIdOrderByCreatedAtDesc(barbershopId)
                .stream()
                .map(orderMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDTO updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado: " + orderId));

        // Se cancelando, devolver estoque
        if (newStatus == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElse(null);
                if (product != null) {
                    product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                    productRepository.save(product);

                    StockMovement movement = StockMovement.builder()
                            .productId(product.getId())
                            .type(MovementType.IN)
                            .quantity(item.getQuantity())
                            .orderId(orderId)
                            .reason("Cancelamento de pedido")
                            .build();
                    stockMovementRepository.save(movement);
                }
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Pedido atualizado: id={}, status={}", saved.getId(), newStatus);
        return orderMapper.toDTO(saved);
    }
}
