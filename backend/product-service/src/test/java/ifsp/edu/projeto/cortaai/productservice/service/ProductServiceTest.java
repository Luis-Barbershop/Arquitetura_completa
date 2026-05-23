package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryPageDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.productservice.feign.UserServiceClient;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private VStockHealthAlertRepository vStockHealthAlertRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldCreateProductWithDefaultsAndInitialStockMovement() {
        UUID shopId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateProductDTO request = new CreateProductDTO(
                shopId,
                "Pomada",
                "Modeladora",
                new BigDecimal("39.90"),
                null,
                null,
                12,
                null,
                "https://cdn.test/pomada.png"
        );
        Product saved = Product.builder()
                .id(productId)
                .barbershopId(shopId)
                .name("Pomada")
                .description("Modeladora")
                .price(new BigDecimal("39.90"))
                .category(ProductCategory.OTHER)
                .stockQuantity(12)
                .minStockQuantity(0)
                .imageUrl("https://cdn.test/pomada.png")
                .active(true)
                .build();
        ProductDTO dto = new ProductDTO(productId, shopId, "Pomada", "Modeladora",
                new BigDecimal("39.90"), null, null, ProductCategory.OTHER, 12, 0,
                "https://cdn.test/pomada.png", true, null);

        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productMapper.toDTO(saved)).thenReturn(dto);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isEqualTo(dto);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getCategory()).isEqualTo(ProductCategory.OTHER);
        assertThat(productCaptor.getValue().getMinStockQuantity()).isZero();

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getType()).isEqualTo(MovementType.IN);
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(12);
        assertThat(movementCaptor.getValue().getReason()).isEqualTo("Estoque inicial");
    }

    @Test
    void shouldCreateProductWithDynamicCategoryAndWithoutInitialStockMovement() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Pomadas")
                .build();
        CreateProductDTO request = new CreateProductDTO(
                shopId,
                "Pomada sem estoque",
                null,
                new BigDecimal("35.00"),
                categoryId,
                ProductCategory.POMADE,
                null,
                3,
                null
        );
        Product saved = Product.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .name("Pomada sem estoque")
                .price(new BigDecimal("35.00"))
                .dynamicCategory(category)
                .category(ProductCategory.POMADE)
                .stockQuantity(0)
                .minStockQuantity(3)
                .active(true)
                .build();
        ProductDTO dto = new ProductDTO(saved.getId(), shopId, saved.getName(), null,
                saved.getPrice(), categoryId, "Pomadas", ProductCategory.POMADE, 0, 3,
                null, true, null);

        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productMapper.toDTO(saved)).thenReturn(dto);

        ProductDTO result = productService.createProduct(request);

        assertThat(result).isEqualTo(dto);
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getDynamicCategory()).isEqualTo(category);
        assertThat(productCaptor.getValue().getStockQuantity()).isZero();
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void shouldListProductsByBarbershop() {
        UUID shopId = UUID.randomUUID();
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .name("Shampoo")
                .active(true)
                .build();
        ProductDTO dto = new ProductDTO(product.getId(), shopId, "Shampoo", null,
                BigDecimal.TEN, null, null, ProductCategory.SHAMPOO, 4, 1,
                null, true, null);

        when(productRepository.findByBarbershopIdAndActiveTrue(shopId)).thenReturn(List.of(product));
        when(productMapper.toDTO(product)).thenReturn(dto);

        assertThat(productService.getProductsByBarbershop(shopId)).containsExactly(dto);
    }

    @Test
    void shouldReturnProductByIdAndRejectMissingProduct() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).name("Gel").build();
        ProductDTO dto = new ProductDTO(productId, UUID.randomUUID(), "Gel", null,
                BigDecimal.TEN, null, null, ProductCategory.OTHER, 1, 0,
                null, true, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toDTO(product)).thenReturn(dto);

        assertThat(productService.getById(productId)).isEqualTo(dto);

        UUID missingId = UUID.randomUUID();
        when(productRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(missingId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado: " + missingId);
    }

    @Test
    void shouldUpdateProductAndRegisterStockOutWhenQuantityDecreases() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .barbershopId(UUID.randomUUID())
                .name("Shampoo")
                .description("Original")
                .price(new BigDecimal("25.00"))
                .category(ProductCategory.SHAMPOO)
                .stockQuantity(10)
                .minStockQuantity(2)
                .active(true)
                .build();
        UpdateProductDTO request = new UpdateProductDTO(
                "Shampoo Premium",
                "Atualizado",
                new BigDecimal("32.00"),
                null,
                ProductCategory.BEARD_OIL,
                4,
                1,
                "https://cdn.test/shampoo.png",
                false
        );
        ProductDTO dto = new ProductDTO(productId, product.getBarbershopId(), "Shampoo Premium", "Atualizado",
                new BigDecimal("32.00"), null, null, ProductCategory.BEARD_OIL, 4, 1,
                "https://cdn.test/shampoo.png", false, null);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDTO(product)).thenReturn(dto);

        ProductDTO result = productService.updateProduct(productId, request);

        assertThat(result).isEqualTo(dto);
        assertThat(product.getName()).isEqualTo("Shampoo Premium");
        assertThat(product.getStockQuantity()).isEqualTo(4);
        assertThat(product.isActive()).isFalse();

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getType()).isEqualTo(MovementType.OUT);
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(6);
    }

    @Test
    void shouldUpdateProductDynamicCategoryAndRegisterStockInWhenQuantityIncreases() {
        UUID productId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Finalizadores")
                .build();
        Product product = Product.builder()
                .id(productId)
                .barbershopId(shopId)
                .name("Pomada")
                .price(BigDecimal.TEN)
                .stockQuantity(2)
                .minStockQuantity(1)
                .active(true)
                .build();
        UpdateProductDTO request = new UpdateProductDTO(
                null,
                null,
                null,
                categoryId,
                null,
                8,
                null,
                null,
                null
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDTO(product)).thenReturn(new ProductDTO(productId, shopId, "Pomada", null,
                BigDecimal.TEN, categoryId, "Finalizadores", null, 8, 1,
                null, true, null));

        productService.updateProduct(productId, request);

        assertThat(product.getDynamicCategory()).isEqualTo(category);
        assertThat(product.getStockQuantity()).isEqualTo(8);
        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getType()).isEqualTo(MovementType.IN);
        assertThat(movementCaptor.getValue().getQuantity()).isEqualTo(6);
    }

    @Test
    void shouldRejectUpdatingMissingProductAndMissingDynamicCategory() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(productId, new UpdateProductDTO(
                "Nome", null, null, null, null, null, null, null, null
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado: " + productId);

        UUID existingProductId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.builder()
                .id(existingProductId)
                .barbershopId(shopId)
                .stockQuantity(1)
                .build();

        when(productRepository.findById(existingProductId)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(existingProductId, new UpdateProductDTO(
                null, null, null, categoryId, null, null, null, null, null
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Categoria não encontrada: " + categoryId);
    }

    @Test
    void shouldBuildInventoryPageWithLowStockFlagAndSafePagination() {
        UUID shopId = UUID.randomUUID();
        Product lowStockProduct = Product.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .name("Gel")
                .price(BigDecimal.TEN)
                .category(ProductCategory.OTHER)
                .stockQuantity(2)
                .minStockQuantity(2)
                .active(true)
                .build();

        when(productRepository.findInventoryPageByFilters(eq(shopId), eq("gel"), eq(ProductCategory.OTHER), eq(null), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lowStockProduct)));

        InventoryPageDTO page = productService.getInventoryPage(shopId, " gel ", ProductCategory.OTHER, null, true, -1, 500);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(100);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).lowStock()).isTrue();
    }

    @Test
    void shouldBuildInventoryPageWithoutLowStockWhenValuesAreMissing() {
        UUID shopId = UUID.randomUUID();
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .name("Sem mínimo")
                .price(BigDecimal.ONE)
                .category(ProductCategory.OTHER)
                .stockQuantity(null)
                .minStockQuantity(null)
                .active(true)
                .build();

        when(productRepository.findInventoryPageByFilters(eq(shopId), eq(null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        InventoryPageDTO page = productService.getInventoryPage(shopId, "   ", null, null, null, 1, 0);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.lowStock()).isFalse();
            assertThat(item.categoryId()).isNull();
            assertThat(item.categoryName()).isNull();
        });
    }

    @Test
    void shouldCalculateFinancialSummaryFromActiveInventoryAndStockInputs() {
        UUID shopId = UUID.randomUUID();
        UUID activeProductId = UUID.randomUUID();
        UUID inactiveProductId = UUID.randomUUID();
        Product active = Product.builder()
                .id(activeProductId)
                .barbershopId(shopId)
                .price(new BigDecimal("20.00"))
                .stockQuantity(3)
                .active(true)
                .build();
        Product inactive = Product.builder()
                .id(inactiveProductId)
                .barbershopId(shopId)
                .price(new BigDecimal("99.00"))
                .stockQuantity(10)
                .active(false)
                .build();
        StockMovement input = StockMovement.builder()
                .productId(activeProductId)
                .type(MovementType.IN)
                .quantity(5)
                .build();
        StockMovement orphanInput = StockMovement.builder()
                .productId(UUID.randomUUID())
                .type(MovementType.IN)
                .quantity(2)
                .build();

        when(productRepository.findByBarbershopId(shopId)).thenReturn(List.of(active, inactive));
        when(stockMovementRepository.findByProductIdsAndTypeAndCreatedAtBetween(any(), eq(MovementType.IN), any(), any()))
                .thenReturn(List.of(input, orphanInput));

        InventoryFinancialSummaryDTO summary = productService.getFinancialSummary(
                shopId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4)
        );

        assertThat(summary.inventoryAssetValue()).isEqualByComparingTo("60.00");
        assertThat(summary.productExpenses()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldReturnEmptyFinancialSummaryWhenBarbershopHasNoProducts() {
        UUID shopId = UUID.randomUUID();
        when(productRepository.findByBarbershopId(shopId)).thenReturn(List.of());

        InventoryFinancialSummaryDTO summary = productService.getFinancialSummary(shopId, null, null);

        assertThat(summary.barbershopId()).isEqualTo(shopId);
        assertThat(summary.productExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.inventoryAssetValue()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(stockMovementRepository, never()).findByProductIdsAndTypeAndCreatedAtBetween(any(), any(), any(), any());
    }

    @Test
    void shouldSoftDeleteProduct() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .active(true)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void shouldRejectDeletingMissingProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado: " + productId);
    }

    @Test
    void shouldRegisterStockSaleAndDecreaseProductQuantity() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .stockQuantity(5)
                .build();
        StockMovement saved = StockMovement.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .type(MovementType.OUT_SALE)
                .quantity(2)
                .unitSalePrice(new BigDecimal("19.90"))
                .notes("Venda balcão")
                .reason("Venda")
                .createdAt(LocalDateTime.of(2026, 5, 21, 10, 0))
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);

        StockMovementDTO result = productService.createStockMovement(new StockMovementRequestDTO(
                productId,
                MovementType.OUT_SALE,
                2,
                new BigDecimal("19.90"),
                "Venda balcão"
        ));

        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(result.type()).isEqualTo(MovementType.OUT_SALE);
        assertThat(result.reason()).isEqualTo("Venda");
        verify(productRepository).save(product);
    }

    @Test
    void shouldRegisterStockEntryWhenCurrentQuantityIsNull() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .stockQuantity(null)
                .build();
        StockMovement saved = StockMovement.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .type(MovementType.IN)
                .quantity(7)
                .reason("Entrada")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);

        StockMovementDTO result = productService.createStockMovement(new StockMovementRequestDTO(
                productId,
                MovementType.IN,
                7,
                null,
                "Compra"
        ));

        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(result.reason()).isEqualTo("Entrada");
        verify(productRepository).save(product);
    }

    @Test
    void shouldResolveAllNonSaleMovementReasons() {
        assertMovementReason(MovementType.RETURN, "Devolução", 4, 6);
        assertMovementReason(MovementType.OUT, "Saída", 4, 2);
        assertMovementReason(MovementType.OUT_CONSUMPTION, "Consumo interno", 4, 2);
        assertMovementReason(MovementType.LOSS, "Perda / Descarte", 4, 2);
    }

    @Test
    void shouldRejectSaleMovementWithoutUnitPrice() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .stockQuantity(5)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.createStockMovement(new StockMovementRequestDTO(
                productId,
                MovementType.OUT_SALE,
                1,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unitSalePrice é obrigatório para venda.");

        verify(stockMovementRepository, never()).save(any());
        verify(productRepository, never()).save(product);
    }

    @Test
    void shouldRejectStockMovementThatWouldMakeQuantityNegative() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .stockQuantity(1)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.createStockMovement(new StockMovementRequestDTO(
                productId,
                MovementType.OUT_CONSUMPTION,
                2,
                null,
                "Uso interno"
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Quantidade insuficiente em estoque");

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void shouldRejectStockMovementForMissingProduct() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createStockMovement(new StockMovementRequestDTO(
                productId,
                MovementType.IN,
                1,
                null,
                null
        )))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Produto não encontrado: " + productId);
    }

    @Test
    void shouldReturnStockMovementHistoryWithSafePagination() {
        UUID productId = UUID.randomUUID();
        StockMovement movement = StockMovement.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .type(MovementType.OUT)
                .quantity(3)
                .unitSalePrice(null)
                .notes("Ajuste")
                .reason("Saída")
                .createdAt(LocalDateTime.of(2026, 5, 22, 8, 0))
                .build();

        when(stockMovementRepository.findByProductIdOrderByCreatedAtDesc(eq(productId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));

        List<StockMovementDTO> history = productService.getStockMovementHistory(productId, -5, 250);

        assertThat(history).singleElement().satisfies(dto -> {
            assertThat(dto.id()).isEqualTo(movement.getId());
            assertThat(dto.reason()).isEqualTo("Saída");
            assertThat(dto.quantity()).isEqualTo(3);
        });
    }

    @Test
    void shouldCreateCategoryWithNormalizedName() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category saved = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Linha Premium")
                .build();

        when(categoryRepository.existsByNameIgnoreCaseAndBarbershopId("Linha Premium", shopId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponseDTO result = productService.createCategory(shopId, new CategoryRequestDTO("  Linha   Premium  "));

        assertThat(result.id()).isEqualTo(categoryId);
        assertThat(result.name()).isEqualTo("Linha Premium");
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Linha Premium");
        assertThat(captor.getValue().getBarbershopId()).isEqualTo(shopId);
    }

    @Test
    void shouldRejectDuplicatedCategoryCreation() {
        UUID shopId = UUID.randomUUID();
        when(categoryRepository.existsByNameIgnoreCaseAndBarbershopId("Pomadas", shopId)).thenReturn(true);

        assertThatThrownBy(() -> productService.createCategory(shopId, new CategoryRequestDTO("Pomadas")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Categoria já existe para esta barbearia.");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCategoryWhenNameIsAvailable() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Antiga")
                .build();
        Category saved = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Nova Categoria")
                .build();

        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndBarbershopId("Nova Categoria", shopId)).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(saved);

        CategoryResponseDTO result = productService.updateCategory(shopId, categoryId, new CategoryRequestDTO(" Nova   Categoria "));

        assertThat(category.getName()).isEqualTo("Nova Categoria");
        assertThat(result.name()).isEqualTo("Nova Categoria");
    }

    @Test
    void shouldUpdateCategoryWithoutDuplicateCheckWhenNameOnlyChangesCase() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Pomadas")
                .build();

        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponseDTO result = productService.updateCategory(shopId, categoryId, new CategoryRequestDTO("pomadas"));

        assertThat(result.name()).isEqualTo("pomadas");
        verify(categoryRepository, never()).existsByNameIgnoreCaseAndBarbershopId(any(), any());
    }

    @Test
    void shouldRejectDuplicatedCategoryUpdateAndMissingCategory() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Antiga")
                .build();

        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndBarbershopId("Nova", shopId)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateCategory(shopId, categoryId, new CategoryRequestDTO("Nova")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Categoria já existe para esta barbearia.");

        UUID missingId = UUID.randomUUID();
        when(categoryRepository.findByIdAndBarbershopId(missingId, shopId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateCategory(shopId, missingId, new CategoryRequestDTO("Qualquer")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Categoria não encontrada: " + missingId);
    }

    @Test
    void shouldListCategoriesAndNormalizeNullName() {
        UUID shopId = UUID.randomUUID();
        Category category = Category.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .name("Produtos")
                .build();

        when(categoryRepository.findByBarbershopIdOrderByNameAsc(shopId)).thenReturn(List.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndBarbershopId("", shopId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        assertThat(productService.getCategories(shopId)).singleElement()
                .satisfies(dto -> assertThat(dto.name()).isEqualTo("Produtos"));
        assertThat(productService.createCategory(shopId, new CategoryRequestDTO(null)).name()).isEmpty();
    }

    @Test
    void shouldRejectDeletingMissingCategory() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteCategory(shopId, categoryId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Categoria não encontrada: " + categoryId);
    }

    @Test
    void shouldRejectDeletingCategoryWithActiveProducts() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Pomadas")
                .build();

        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(productRepository.existsByDynamicCategoryIdAndActiveTrue(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> productService.deleteCategory(shopId, categoryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Categoria possui produtos ativos. Reclassifique antes de excluir.");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void shouldDeleteCategoryWithoutActiveProducts() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .barbershopId(shopId)
                .name("Sem produtos")
                .build();

        when(categoryRepository.findByIdAndBarbershopId(categoryId, shopId)).thenReturn(Optional.of(category));
        when(productRepository.existsByDynamicCategoryIdAndActiveTrue(categoryId)).thenReturn(false);

        productService.deleteCategory(shopId, categoryId);

        verify(categoryRepository).delete(category);
    }

    @Test
    void shouldMapStockHealthAlertsFromView() {
        UUID shopId = UUID.randomUUID();
        VStockHealthAlertRepository.StockHealthAlertProjection restockAlert =
                stockHealthAlert("p1", "Pomada", "OTHER", 1, 3, 1);
        VStockHealthAlertRepository.StockHealthAlertProjection healthyAlert =
                stockHealthAlert("p2", "Shampoo", "SHAMPOO", 8, 2, null);

        when(vStockHealthAlertRepository.findByBarbershopId(shopId.toString()))
                .thenReturn(List.of(restockAlert, healthyAlert));

        List<StockHealthAlertResponseDTO> alerts = productService.getStockHealthAlert(shopId);

        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).requiresRestock()).isTrue();
        assertThat(alerts.get(1).requiresRestock()).isFalse();
    }

    @Test
    void shouldAllowOwnerToUseSecuredStockEndpoints() {
        UUID shopId = UUID.randomUUID();
        UserInfoDTO owner = ownerUser(shopId);
        Product product = Product.builder()
                .id(UUID.randomUUID())
                .barbershopId(shopId)
                .name("Pomada")
                .active(true)
                .build();
        ProductDTO dto = new ProductDTO(product.getId(), shopId, "Pomada", null,
                BigDecimal.TEN, null, null, ProductCategory.OTHER, 2, 1,
                null, true, null);

        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(owner);
        when(productRepository.findByBarbershopIdAndActiveTrue(shopId)).thenReturn(List.of(product));
        when(productMapper.toDTO(product)).thenReturn(dto);

        assertThat(productService.getProductsByBarbershop("owner-uid", shopId)).containsExactly(dto);
    }

    @Test
    void shouldRejectSecuredStockAccessForAnotherBarbershop() {
        UUID ownerShopId = UUID.randomUUID();
        UUID requestedShopId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("owner-uid")).thenReturn(ownerUser(ownerShopId));

        assertThatThrownBy(() -> productService.getCategories("owner-uid", requestedShopId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private void assertMovementReason(MovementType type, String expectedReason, int initialQuantity, int expectedQuantity) {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .stockQuantity(initialQuantity)
                .build();
        StockMovement saved = StockMovement.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .type(type)
                .quantity(2)
                .reason(expectedReason)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any(StockMovement.class))).thenReturn(saved);

        StockMovementDTO result = productService.createStockMovement(new StockMovementRequestDTO(
                productId,
                type,
                2,
                null,
                null
        ));

        assertThat(product.getStockQuantity()).isEqualTo(expectedQuantity);
        assertThat(result.reason()).isEqualTo(expectedReason);
    }

    private UserInfoDTO ownerUser(UUID barbershopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(UUID.randomUUID());
        user.setUserType("BARBER");
        user.setRole("ROLE_OWNER");
        user.setBarbershopId(barbershopId);
        return user;
    }

    private VStockHealthAlertRepository.StockHealthAlertProjection stockHealthAlert(String productId,
                                                                                   String productName,
                                                                                   String category,
                                                                                   Integer currentStock,
                                                                                   Integer predictedMinimum,
                                                                                   Integer requiresRestock) {
        return new VStockHealthAlertRepository.StockHealthAlertProjection() {
            @Override
            public String getProductId() {
                return productId;
            }

            @Override
            public String getProductName() {
                return productName;
            }

            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public Integer getCurrentStock() {
                return currentStock;
            }

            @Override
            public Integer getPredictedMinimum() {
                return predictedMinimum;
            }

            @Override
            public Integer getRequiresRestock() {
                return requiresRestock;
            }
        };
    }
}
