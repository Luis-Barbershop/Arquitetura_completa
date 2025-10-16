package ifsp.edu.projeto.cortaai.controller;

import ifsp.edu.projeto.cortaai.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.dto.CustomerDTO;
import ifsp.edu.projeto.cortaai.service.BarberService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ifsp.edu.projeto.cortaai.dto.CreateBarberDTO;

@CrossOrigin
@RestController
@RequestMapping(value = "/api/barbers", produces = MediaType.APPLICATION_JSON_VALUE)
public class BarberController {

    private final BarberService barberService; // Use a interface

    public BarberController(final BarberService barberService) {
        this.barberService = barberService;
    }

    @GetMapping("/{id}/customers")
    public ResponseEntity<List<CustomerDTO>> getCustomerHistory(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(barberService.findCustomerHistory(id));
    }

    @GetMapping
    public ResponseEntity<List<BarberDTO>> getAllBarbers() {
        return ResponseEntity.ok(barberService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberDTO> getBarber(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(barberService.get(id));
    }

    @PostMapping("/create")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createBarber(@RequestBody @Valid final CreateBarberDTO createBarberDTO) {
        final UUID createdId = barberService.create(createBarberDTO);
        return new ResponseEntity<>(createdId, HttpStatus.CREATED);
    }


    @PutMapping("/{id}")
    public ResponseEntity<UUID> updateBarber(@PathVariable(name = "id") final UUID id,
                                             @RequestBody @Valid final BarberDTO barberDTO) {
        barberService.update(id, barberDTO);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteBarber(@PathVariable(name = "id") final UUID id) {
        barberService.delete(id);
        return ResponseEntity.noContent().build();
    }

}