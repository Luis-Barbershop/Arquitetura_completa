package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.service.CustomerService;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerCreateDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/customers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Customers", description = "Endpoints para gerenciamento de clientes e autenticação")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(final CustomerService customerService) {
        this.customerService = customerService;
    }

    @Operation(summary = "Lista todos os clientes", description = "Retorna uma lista com todos os clientes cadastrados no sistema.")
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @Operation(summary = "Busca um cliente por ID", description = "Retorna os detalhes de um cliente específico com base no seu UUID.")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(
            @Parameter(description = "UUID do cliente") @PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    @Operation(summary = "Registra um novo cliente", description = "Cria um novo cliente. Aceita upload de foto de perfil junto com os dados de registro (multipart/form-data).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao processar o upload do arquivo")
    })
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> createCustomer(
            @Parameter(description = "Dados de criação do cliente") @RequestPart("customer") @Valid final CustomerCreateDTO customerCreateDTO,
            @Parameter(description = "Foto de perfil (opcional)") @RequestPart(value = "file", required = false) final MultipartFile file) {
        try {
            final UUID createdId = customerService.create(customerCreateDTO, file);
            return new ResponseEntity<>(createdId, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Realiza o login do cliente", description = "Autentica o cliente utilizando email e senha e retorna um token JWT para acesso aos endpoints protegidos.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Parameter(description = "Credenciais do cliente") @RequestBody @Valid final LoginDTO loginDTO) {
        final LoginResponseDTO loginResponse = customerService.login(loginDTO);
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Atualiza o perfil do cliente logado", description = "Atualiza os dados do cliente. Utiliza o token JWT (Principal) para identificar o usuário, não requer ID na URL por segurança.")
    @PutMapping("/me")
    public ResponseEntity<Void> updateCustomer(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Novos dados do cliente") @RequestBody @Valid final CustomerDTO customerDTO) {
        customerService.update(principal.getName(), customerDTO);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Deleta o perfil do cliente logado", description = "Exclui a conta do cliente autenticado com base no token JWT (Principal).")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCustomer(@Parameter(hidden = true) Principal principal) {
        customerService.delete(principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Faz o upload/atualização da foto de perfil", description = "Permite que o cliente logado atualize apenas a sua foto de perfil separadamente do registro.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload realizado com sucesso (retorna a URL da imagem)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao fazer o upload")
    })
    @PostMapping(value = "/me/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadCustomerPhoto(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "Arquivo da imagem") @RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = customerService.updateProfilePhoto(principal.getName(), file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }
}