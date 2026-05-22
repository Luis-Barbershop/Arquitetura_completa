package ifsp.edu.projeto.cortaai.productservice.service;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryPageDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
}
