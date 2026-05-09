package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.userservice.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de gerenciamento de perfil de clientes.
 *
 * <p>Registro e login foram movidos para {@link AuthController} ({@code /api/auth/verify}).
 * O usuário é identificado pelo header {@code X-User-UID} injetado pelo API Gateway
 * após validação do Firebase ID Token.
 */
@RestController
@RequestMapping(value = "/api/customers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Customers", description = "Endpoints de perfil de clientes")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(final CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Lista todos os clientes")
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @Operation(summary = "Busca um cliente por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(
            @Parameter(description = "UUID do cliente") @PathVariable UUID id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    @Operation(
            summary = "Atualiza o perfil do cliente logado",
            description = "Identifica o cliente pelo header `X-User-UID` injetado pelo Gateway."
    )
    @PutMapping("/me")
    public ResponseEntity<Void> updateCustomer(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestBody @Valid UpdateCustomerDTO dto) {
        customerService.updateByFirebaseUid(firebaseUid, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deleta o perfil do cliente logado")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        customerService.deleteByFirebaseUid(firebaseUid);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lista favoritas do cliente logado")
    @GetMapping("/me/favorites")
    public ResponseEntity<List<UUID>> listMyFavorites(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        return ResponseEntity.ok(customerService.listFavoriteBarbershopIdsByFirebaseUid(firebaseUid));
    }

    @Operation(summary = "Favorita uma barbearia para o cliente logado")
    @PostMapping("/me/favorites/{barbershopId}")
    public ResponseEntity<Void> addFavorite(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @Parameter(description = "UUID da barbearia") @PathVariable UUID barbershopId) {
        customerService.addFavoriteBarbershopByFirebaseUid(firebaseUid, barbershopId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove uma barbearia das favoritas do cliente logado")
    @DeleteMapping("/me/favorites/{barbershopId}")
    public ResponseEntity<Void> removeFavorite(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @Parameter(description = "UUID da barbearia") @PathVariable UUID barbershopId) {
        customerService.removeFavoriteBarbershopByFirebaseUid(firebaseUid, barbershopId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload/atualização da foto de perfil do cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso (retorna a URL da imagem)"),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente ou inválido",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno ao fazer o upload",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping(value = "/me/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadCustomerPhoto(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = customerService.updateProfilePhotoByFirebaseUid(firebaseUid, file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Falha no upload: " + e.getMessage());
        }
    }
}
