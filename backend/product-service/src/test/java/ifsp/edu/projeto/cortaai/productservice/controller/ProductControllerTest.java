package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.CategoryRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryPageDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryProductItemDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.model.MovementType;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import ifsp.edu.projeto.cortaai.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private static final String FIREBASE_UID = "firebase-uid";

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @InjectMocks
    private InternalProductController internalProductController;

    @Test
    void shouldCreateProductWithCreatedStatus() {
        UUID shopId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        CreateProductDTO request = new CreateProductDTO(shopId, "Pomada", null,
                new BigDecimal("30.00"), null, ProductCategory.OTHER, 5, 1, null);
        ProductDTO dto = productDto(productId, shopId);

        when(productService.createProduct(FIREBASE_UID, request)).thenReturn(dto);

        ResponseEntity<ProductDTO> response = productController.createProduct(FIREBASE_UID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void shouldDelegateProductQueriesAndUpdates() {
        UUID shopId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        ProductDTO dto = productDto(productId, shopId);
        InventoryPageDTO page = new InventoryPageDTO(List.of(new InventoryProductItemDTO(
                productId, "Pomada", categoryId, "Finalizadores", ProductCategory.OTHER,
                BigDecimal.TEN, 3, 2, false, true)), 0, 20, 1, 1);
        UpdateProductDTO update = new UpdateProductDTO("Nova", null, null, null,
                null, null, null, null, null);

        when(productService.getProductsByBarbershop(FIREBASE_UID, shopId)).thenReturn(List.of(dto));
        when(productService.getInventoryPage(FIREBASE_UID, shopId, "pom", ProductCategory.OTHER, categoryId, true, 0, 20))
                .thenReturn(page);
        when(productService.getById(FIREBASE_UID, productId)).thenReturn(dto);
        when(productService.updateProduct(FIREBASE_UID, productId, update)).thenReturn(dto);

        assertThat(productController.getProductsByBarbershop(FIREBASE_UID, shopId).getBody()).containsExactly(dto);
        assertThat(productController.getInventoryPage(FIREBASE_UID, shopId, "pom", ProductCategory.OTHER, categoryId, true, 0, 20).getBody())
                .isEqualTo(page);
        assertThat(productController.getById(FIREBASE_UID, productId).getBody()).isEqualTo(dto);
        assertThat(productController.updateProduct(FIREBASE_UID, productId, update).getBody()).isEqualTo(dto);
    }

    @Test
    void shouldDeleteProductWithNoContentStatus() {
        UUID productId = UUID.randomUUID();

        ResponseEntity<Void> response = productController.deleteProduct(FIREBASE_UID, productId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).deleteProduct(FIREBASE_UID, productId);
    }

    @Test
    void shouldDelegateStockMovementEndpoints() {
        UUID productId = UUID.randomUUID();
        StockMovementDTO movement = new StockMovementDTO(UUID.randomUUID(), productId, MovementType.IN,
                4, null, "Compra", "Entrada", LocalDateTime.of(2026, 5, 22, 9, 0));
        StockMovementRequestDTO request = new StockMovementRequestDTO(productId, MovementType.IN, 4, null, "Compra");

        when(productService.getStockMovementHistory(FIREBASE_UID, productId, 0, 50)).thenReturn(List.of(movement));
        when(productService.createStockMovement(FIREBASE_UID, request)).thenReturn(movement);

        assertThat(productController.getStockMovements(FIREBASE_UID, productId, 0, 50).getBody()).containsExactly(movement);
        ResponseEntity<StockMovementDTO> response = productController.createStockMovement(FIREBASE_UID, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(movement);
    }

    @Test
    void shouldDelegateCategoryEndpoints() {
        UUID shopId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CategoryRequestDTO request = new CategoryRequestDTO("Pomadas");
        CategoryResponseDTO responseDto = new CategoryResponseDTO(categoryId, "Pomadas", shopId);

        when(productService.getCategories(FIREBASE_UID, shopId)).thenReturn(List.of(responseDto));
        when(productService.createCategory(FIREBASE_UID, shopId, request)).thenReturn(responseDto);
        when(productService.updateCategory(FIREBASE_UID, shopId, categoryId, request)).thenReturn(responseDto);

        assertThat(productController.getCategories(FIREBASE_UID, shopId).getBody()).containsExactly(responseDto);
        assertThat(productController.createCategory(FIREBASE_UID, shopId, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(productController.createCategory(FIREBASE_UID, shopId, request).getBody()).isEqualTo(responseDto);
        assertThat(productController.updateCategory(FIREBASE_UID, shopId, categoryId, request).getBody()).isEqualTo(responseDto);

        ResponseEntity<Void> deleteResponse = productController.deleteCategory(FIREBASE_UID, shopId, categoryId);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).deleteCategory(FIREBASE_UID, shopId, categoryId);
    }

    @Test
    void shouldDelegateAnalyticsAndInternalFinancialSummary() {
        UUID shopId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 22);
        StockHealthAlertResponseDTO alert = new StockHealthAlertResponseDTO("p1", "Pomada",
                "OTHER", 1, 3, true);
        InventoryFinancialSummaryDTO summary = new InventoryFinancialSummaryDTO(shopId,
                new BigDecimal("50.00"), new BigDecimal("120.00"));

        when(productService.getStockHealthAlert(FIREBASE_UID, shopId)).thenReturn(List.of(alert));
        when(productService.getFinancialSummary(shopId, from, to)).thenReturn(summary);

        assertThat(productController.getStockHealthAlert(FIREBASE_UID, shopId).getBody()).containsExactly(alert);
        assertThat(internalProductController.getFinancialSummary(shopId, from, to).getBody()).isEqualTo(summary);
    }

    private ProductDTO productDto(UUID productId, UUID shopId) {
        return new ProductDTO(productId, shopId, "Pomada", null, BigDecimal.TEN,
                null, null, ProductCategory.OTHER, 3, 1, null, true, null);
    }
}
