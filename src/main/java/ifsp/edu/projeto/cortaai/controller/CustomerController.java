package ifsp.edu.projeto.cortaai.controller;

import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.service.CustomerService;
import ifsp.edu.projeto.cortaai.dto.CustomerCreateDTO;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ifsp.edu.projeto.cortaai.dto.LoginDTO;

@CrossOrigin
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

    @PostMapping("/create")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createCustomer(@RequestBody @Valid final CustomerCreateDTO customerCreateDTO) {
        final UUID createdId = customerService.create(customerCreateDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }


    @PostMapping("/login")
    public ResponseEntity<CustomerDTO> login(@RequestBody @Valid final LoginDTO loginDTO) {
        final CustomerDTO customer = customerService.login(loginDTO);
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateCustomer(@PathVariable(name = "id") final UUID id,
            @RequestBody @Valid final CustomerDTO customerDTO) {
        customerService.update(id, customerDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteCustomer(@PathVariable(name = "id") final UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
