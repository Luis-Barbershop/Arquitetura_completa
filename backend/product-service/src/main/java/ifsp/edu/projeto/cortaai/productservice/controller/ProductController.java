package ifsp.edu.projeto.cortaai.productservice.controller;

import ifsp.edu.projeto.cortaai.productservice.dto.CreateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.ProductDTO;
import ifsp.edu.projeto.cortaai.productservice.dto.UpdateProductDTO;
import ifsp.edu.projeto.cortaai.productservice.service.ProductService;
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
public class ProductController {

    private final ProductService productService;

    /**
     * Cria um novo produto.
     */
    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@Valid @RequestBody CreateProductDTO dto) {
        ProductDTO product = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    /**
     * Lista produtos de uma barbearia (somente ativos).
     */
    @GetMapping
    public ResponseEntity<List<ProductDTO>> getProductsByBarbershop(
            @RequestParam UUID barbershopId) {
        return ResponseEntity.ok(productService.getProductsByBarbershop(barbershopId));
    }

    /**
     * Busca um produto por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Atualiza um produto.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    /**
     * Desativa (soft delete) um produto.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
