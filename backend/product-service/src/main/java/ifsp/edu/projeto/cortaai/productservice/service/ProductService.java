package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.mapper.ProductMapper;
import ifsp.edu.projeto.cortaai.productservice.model.MovementType;
import ifsp.edu.projeto.cortaai.productservice.model.Product;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import ifsp.edu.projeto.cortaai.productservice.model.StockMovement;
import ifsp.edu.projeto.cortaai.productservice.repository.ProductRepository;
import ifsp.edu.projeto.cortaai.productservice.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductDTO createProduct(CreateProductDTO dto) {
        Product product = Product.builder()
                .barbershopId(dto.barbershopId())
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .category(dto.category() != null ? dto.category() : ProductCategory.OTHER)
                .stockQuantity(dto.stockQuantity() != null ? dto.stockQuantity() : 0)
                .minStockQuantity(dto.minStockQuantity() != null ? dto.minStockQuantity() : 0)
                .imageUrl(dto.imageUrl())
                .active(true)
                .build();

        Product saved = productRepository.save(product);

        // Se tem estoque inicial, registrar movimentação
        if (saved.getStockQuantity() > 0) {
            StockMovement movement = StockMovement.builder()
                    .productId(saved.getId())
                    .type(MovementType.IN)
                    .quantity(saved.getStockQuantity())
                    .reason("Estoque inicial")
                    .build();
            stockMovementRepository.save(movement);
        }

        log.info("Produto criado: id={}, name={}", saved.getId(), saved.getName());
        return productMapper.toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByBarbershop(UUID barbershopId) {
        return productRepository.findByBarbershopIdAndActiveTrue(barbershopId)
                .stream()
                .map(productMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDTO getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + id));
        return productMapper.toDTO(product);
    }

    @Transactional
    public ProductDTO updateProduct(UUID id, UpdateProductDTO dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + id));

        if (dto.name() != null) product.setName(dto.name());
        if (dto.description() != null) product.setDescription(dto.description());
        if (dto.price() != null) product.setPrice(dto.price());
        if (dto.category() != null) product.setCategory(dto.category());
        if (dto.imageUrl() != null) product.setImageUrl(dto.imageUrl());
        if (dto.active() != null) product.setActive(dto.active());
        if (dto.minStockQuantity() != null) product.setMinStockQuantity(dto.minStockQuantity());

        // Atualizar estoque se necessário
        if (dto.stockQuantity() != null && !dto.stockQuantity().equals(product.getStockQuantity())) {
            int diff = dto.stockQuantity() - product.getStockQuantity();
            MovementType type = diff > 0 ? MovementType.IN : MovementType.OUT;
            StockMovement movement = StockMovement.builder()
                    .productId(product.getId())
                    .type(type)
                    .quantity(Math.abs(diff))
                    .reason("Ajuste manual de estoque")
                    .build();
            stockMovementRepository.save(movement);
            product.setStockQuantity(dto.stockQuantity());
        }

        Product saved = productRepository.save(product);
        log.info("Produto atualizado: id={}", saved.getId());
        return productMapper.toDTO(saved);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Produto desativado: id={}", id);
    }

    @Transactional(readOnly = true)
    public InventoryFinancialSummaryDTO getFinancialSummary(UUID barbershopId, LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now();
        LocalDate endDate = to != null ? to : startDate;
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);

        List<Product> products = productRepository.findByBarbershopId(barbershopId);
        if (products.isEmpty()) {
            return new InventoryFinancialSummaryDTO(barbershopId, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Map<UUID, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal inventoryAssetValue = products.stream()
                .filter(Product::isActive)
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UUID> productIds = products.stream().map(Product::getId).toList();
        BigDecimal productExpenses = stockMovementRepository
                .findByProductIdsAndTypeAndCreatedAtBetween(productIds, MovementType.IN, start, end)
                .stream()
                .map(m -> {
                    Product p = productById.get(m.getProductId());
                    if (p == null) {
                        return BigDecimal.ZERO;
                    }
                    return p.getPrice().multiply(BigDecimal.valueOf(m.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryFinancialSummaryDTO(barbershopId, productExpenses, inventoryAssetValue);
    }
}
