package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.CategoryResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.InventoryPageDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockHealthAlertResponseDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.StockMovementRequestDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.productservice.model.ProductCategory;
import ifsp.edu.projeto.cortaai.productservice.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
 * Controller REST para produtos.
 * Endpoints públicos (expostos via Gateway).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "NOVO: Endpoints para gestão de catálogo de produtos nas barbearias")
public class ProductController {

    private final ProductService productService;

    /**
     * Cria um novo produto.
     */
    @Operation(summary = "Criar produto", description = "Adiciona um novo produto ao catálogo da barbearia.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(
            @Parameter(description = "Dados de criação do produto") @Valid @RequestBody CreateProductDTO dto) {
        ProductDTO product = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Lista produtos de uma barbearia (somente ativos).
     */
    @Operation(summary = "Listar produtos de uma barbearia", description = "Retorna todos os produtos ativos que pertencem ao catálogo de uma barbearia específica.")
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProductsByBarbershop(
            @Parameter(description = "UUID da barbearia") @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(productService.getProductsByBarbershop(barbershopId));
    }

    @Operation(summary = "Inventário paginado", description = "Lista produtos ativos com paginação e filtros de busca, categoria e estoque baixo.")
    @GetMapping("/inventory")
    public ResponseEntity<InventoryPageDTO> getInventoryPage(
            @Parameter(description = "UUID da barbearia") @RequestParam UUID barbershopId,
            @Parameter(description = "Busca por nome/descricao") @RequestParam(required = false) String search,
            @Parameter(description = "Categoria do produto") @RequestParam(required = false) ProductCategory category,
            @Parameter(description = "Categoria dinâmica") @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filtrar apenas estoque baixo") @RequestParam(required = false) Boolean lowStock,
            @Parameter(description = "Página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getInventoryPage(barbershopId, search, category, categoryId, lowStock, page, size));
    }

    /**
     * Busca um produto por ID.
     */
    @Operation(summary = "Buscar produto por ID", description = "Retorna os detalhes de um produto específico através do seu ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(
            @Parameter(description = "UUID do produto") @PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Atualiza um produto.
     */
    @Operation(summary = "Atualizar produto", description = "Atualiza as informações de um produto existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @Parameter(description = "UUID do produto") @PathVariable UUID id,
            @Parameter(description = "Novos dados do produto") @RequestBody UpdateProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    /**
     * Desativa (soft delete) um produto.
     */
    @Operation(summary = "Desativar produto", description = "Realiza um soft delete no produto, ocultando-o das listagens e mantendo o histórico de estoque.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Produto desativado"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "UUID do produto") @PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Histórico de movimentações", description = "Retorna histórico paginado de entradas/saídas de estoque de um produto.")
    @GetMapping("/{id}/movements")
    public ResponseEntity<List<StockMovementDTO>> getStockMovements(
            @Parameter(description = "UUID do produto") @PathVariable UUID id,
            @Parameter(description = "Página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.getStockMovementHistory(id, page, size));
    }

    @Operation(summary = "Registra movimentação de estoque", description = "Registra entrada, consumo interno, venda, perda ou devolução e atualiza a quantidade do produto.")
    @PostMapping("/stock-movements")
    public ResponseEntity<StockMovementDTO> createStockMovement(
            @RequestBody @Valid StockMovementRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createStockMovement(dto));
    }

    @Operation(summary = "Lista categorias dinâmicas", description = "Retorna categorias criadas pelo owner para a barbearia.")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getCategories(@RequestParam UUID barbershopId) {
        return ResponseEntity.ok(productService.getCategories(barbershopId));
    }

    @Operation(summary = "Cria categoria dinâmica", description = "Cria uma categoria de estoque no escopo da barbearia.")
    @PostMapping("/categories")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @RequestParam UUID barbershopId,
            @RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createCategory(barbershopId, dto));
    }

    @Operation(summary = "Atualiza categoria dinâmica", description = "Renomeia uma categoria de estoque da barbearia.")
    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @RequestParam UUID barbershopId,
            @PathVariable UUID id,
            @RequestBody @Valid CategoryRequestDTO dto) {
        return ResponseEntity.ok(productService.updateCategory(barbershopId, id, dto));
    }

    @Operation(summary = "Exclui categoria dinâmica", description = "Exclui categoria se não houver produto ativo vinculado.")
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @RequestParam UUID barbershopId,
            @PathVariable UUID id) {
        productService.deleteCategory(barbershopId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Alertas de saúde do estoque", description = "Retorna todos os produtos com indicação de estoque crítico via view v_stock_health_alert.")
    @GetMapping("/analytics/stock-health")
    public ResponseEntity<List<StockHealthAlertResponseDTO>> getStockHealthAlert(
            @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(productService.getStockHealthAlert(barbershopId));
    }
}
