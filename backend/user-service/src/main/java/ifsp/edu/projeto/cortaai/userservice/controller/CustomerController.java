package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.service.CustomerService;
import ifsp.edu.projeto.cortaai.userservice.dto.CustomerCreateDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginDTO;
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
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(final CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(customerService.get(id));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UUID> createCustomer(
            @RequestPart("customer") @Valid final CustomerCreateDTO customerCreateDTO,
            @RequestPart(value = "file", required = false) final MultipartFile file) {
        try {
            final UUID createdId = customerService.create(customerCreateDTO, file);
            return new ResponseEntity<>(createdId, HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid final LoginDTO loginDTO) { // TIPO DE RETORNO ALTERADO
        final LoginResponseDTO loginResponse = customerService.login(loginDTO); // TIPO DE RETORNO ALTERADO
        return ResponseEntity.ok(loginResponse);
    }

    @PutMapping("/me") // ROTA ALTERADA
    public ResponseEntity<Void> updateCustomer(
            Principal principal, // Usuário autenticado injetado
            @RequestBody @Valid final CustomerDTO customerDTO) {

        // Passa o e-mail (principal.getName()) para o serviço
        customerService.update(principal.getName(), customerDTO);
        return ResponseEntity.ok().build(); // Retorna 200 OK
    }

    @DeleteMapping("/me") // ROTA ALTERADA
    public ResponseEntity<Void> deleteCustomer(Principal principal) {
        customerService.delete(principal.getName()); // Passa o e-mail
        return ResponseEntity.noContent().build();
    }

    // --- NOVO ENDPOINT DE UPLOAD ---
    @PostMapping("/me/upload-photo") // ROTA ALTERADA
    public ResponseEntity<String> uploadCustomerPhoto(
            Principal principal, // Usuário autenticado injetado
            @RequestParam("file") MultipartFile file) {
        try {
            // Passa o e-mail para o serviço
            String imageUrl = customerService.updateProfilePhoto(principal.getName(), file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Falha no upload: " + e.getMessage());
        }
    }

}