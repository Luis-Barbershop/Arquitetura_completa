package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.service.ProductService;
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
            @ApiResponse(responseCode = "201", description = "Produto criado com sucesso")
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

    /**
     * Busca um produto por ID.
     */
    @Operation(summary = "Buscar produto por ID", description = "Retorna os detalhes de um produto específico através do seu ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(
            @Parameter(description = "UUID do produto") @PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Atualiza um produto.
     */
    @Operation(summary = "Atualizar produto", description = "Atualiza as informações de um produto existente.")
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @Parameter(description = "UUID do produto") @PathVariable UUID id,
            @Parameter(description = "Novos dados do produto") @RequestBody UpdateProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    /**
     * Desativa (soft delete) um produto.
     */
    @Operation(summary = "Desativar produto", description = "Realiza um soft delete no produto, ocultando-o das listagens, mas mantendo seu histórico para os pedidos antigos.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "UUID do produto") @PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}