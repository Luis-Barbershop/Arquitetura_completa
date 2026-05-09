package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryPageDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryProductItemDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.mapper.ProductMapper;
import ifsp.edu.projeto.cortaai.productservice.model.Category;
import ifsp.edu.projeto.cortaai.productservice.model.MovementType;
import ifsp.edu.projeto.cortaai.productservice.model.Product;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import ifsp.edu.projeto.cortaai.productservice.model.StockMovement;
import ifsp.edu.projeto.cortaai.productservice.repository.CategoryRepository;
import ifsp.edu.projeto.cortaai.productservice.repository.ProductRepository;
import ifsp.edu.projeto.cortaai.productservice.repository.StockMovementRepository;
import ifsp.edu.projeto.cortaai.productservice.repository.analytics.VStockHealthAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductMapper productMapper;
    private final VStockHealthAlertRepository vStockHealthAlertRepository;

    @Transactional
    public ProductDTO createProduct(CreateProductDTO dto) {
        Category category = resolveCategory(dto.barbershopId(), dto.categoryId());
        Product product = Product.builder()
                .barbershopId(dto.barbershopId())
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .category(dto.category() != null ? dto.category() : ProductCategory.OTHER)
                .dynamicCategory(category)
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
                    .notes("Estoque inicial")
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
    public InventoryPageDTO getInventoryPage(UUID barbershopId,
                                             String search,
                                             ProductCategory category,
                                             UUID categoryId,
                                             Boolean lowStock,
                                             int page,
                                             int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize);

        String searchTerm = (search == null || search.isBlank()) ? null : search.trim();

        Page<Product> result = productRepository.findInventoryPageByFilters(
                barbershopId,
                searchTerm,
                category,
                categoryId,
                lowStock,
                pageable
        );

        List<InventoryProductItemDTO> items = result.getContent().stream()
                .map(this::toInventoryItem)
                .toList();

        return new InventoryPageDTO(items, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
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
        if (dto.categoryId() != null) product.setDynamicCategory(resolveCategory(product.getBarbershopId(), dto.categoryId()));
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
                    .notes("Ajuste manual de estoque")
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

    @Transactional(readOnly = true)
    public List<StockMovementDTO> getStockMovementHistory(UUID productId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize);

        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .getContent()
                .stream()
                .map(m -> new StockMovementDTO(
                        m.getId(),
                        m.getProductId(),
                        m.getType(),
                        m.getQuantity(),
                        m.getUnitSalePrice(),
                        m.getNotes(),
                        m.getReason(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public StockMovementDTO createStockMovement(StockMovementRequestDTO dto) {
        Product product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + dto.productId()));

        MovementType type = dto.type();
        int currentQuantity = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int delta = switch (type) {
            case IN, RETURN -> dto.quantity();
            case OUT, OUT_CONSUMPTION, OUT_SALE, LOSS -> -dto.quantity();
        };
        int nextQuantity = currentQuantity + delta;

        if (type == MovementType.OUT_SALE && dto.unitSalePrice() == null) {
            throw new IllegalArgumentException("unitSalePrice é obrigatório para venda.");
        }
        if (nextQuantity < 0) {
            throw new IllegalStateException("Quantidade insuficiente em estoque");
        }

        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .type(type)
                .quantity(dto.quantity())
                .unitSalePrice(dto.unitSalePrice())
                .notes(dto.notes())
                .reason(resolveMovementReason(type))
                .build();

        product.setStockQuantity(nextQuantity);
        StockMovement saved = stockMovementRepository.save(movement);
        productRepository.save(product);

        return new StockMovementDTO(
                saved.getId(),
                saved.getProductId(),
                saved.getType(),
                saved.getQuantity(),
                saved.getUnitSalePrice(),
                saved.getNotes(),
                saved.getReason(),
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategories(UUID barbershopId) {
        return categoryRepository.findByBarbershopIdOrderByNameAsc(barbershopId).stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponseDTO createCategory(UUID barbershopId, CategoryRequestDTO dto) {
        String name = normalizeCategoryName(dto.name());
        if (categoryRepository.existsByNameIgnoreCaseAndBarbershopId(name, barbershopId)) {
            throw new IllegalStateException("Categoria já existe para esta barbearia.");
        }

        Category saved = categoryRepository.save(Category.builder()
                .barbershopId(barbershopId)
                .name(name)
                .build());

        return toCategoryResponse(saved);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(UUID barbershopId, UUID categoryId, CategoryRequestDTO dto) {
        Category category = categoryRepository.findByIdAndBarbershopId(categoryId, barbershopId)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada: " + categoryId));
        String name = normalizeCategoryName(dto.name());
        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByNameIgnoreCaseAndBarbershopId(name, barbershopId)) {
            throw new IllegalStateException("Categoria já existe para esta barbearia.");
        }

        category.setName(name);
        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID barbershopId, UUID categoryId) {
        Category category = categoryRepository.findByIdAndBarbershopId(categoryId, barbershopId)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada: " + categoryId));

        if (productRepository.existsByDynamicCategoryIdAndActiveTrue(categoryId)) {
            throw new IllegalStateException("Categoria possui produtos ativos. Reclassifique antes de excluir.");
        }

        categoryRepository.delete(category);
    }

    private InventoryProductItemDTO toInventoryItem(Product product) {
        boolean lowStock = product.getStockQuantity() != null
                && product.getMinStockQuantity() != null
                && product.getStockQuantity() <= product.getMinStockQuantity();

        return new InventoryProductItemDTO(
                product.getId(),
                product.getName(),
                product.getDynamicCategory() != null ? product.getDynamicCategory().getId() : null,
                product.getDynamicCategory() != null ? product.getDynamicCategory().getName() : null,
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getMinStockQuantity(),
                lowStock,
                product.isActive()
        );
    }

    @Transactional(readOnly = true)
    public List<StockHealthAlertResponseDTO> getStockHealthAlert(UUID barbershopId) {
        return vStockHealthAlertRepository.findByBarbershopId(barbershopId.toString()).stream()
                .map(p -> new StockHealthAlertResponseDTO(
                        p.getProductId(),
                        p.getProductName(),
                        p.getCategory(),
                        p.getCurrentStock(),
                        p.getPredictedMinimum(),
                        p.getRequiresRestock() != null && p.getRequiresRestock() == 1
                ))
                .toList();
    }

    private Category resolveCategory(UUID barbershopId, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findByIdAndBarbershopId(categoryId, barbershopId)
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada: " + categoryId));
    }

    private CategoryResponseDTO toCategoryResponse(Category category) {
        return new CategoryResponseDTO(category.getId(), category.getName(), category.getBarbershopId());
    }

    private String normalizeCategoryName(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ");
    }

    private String resolveMovementReason(MovementType type) {
        return switch (type) {
            case IN -> "Entrada";
            case RETURN -> "Devolução";
            case OUT -> "Saída";
            case OUT_CONSUMPTION -> "Consumo interno";
            case OUT_SALE -> "Venda";
            case LOSS -> "Perda / Descarte";
        };
    }
}
