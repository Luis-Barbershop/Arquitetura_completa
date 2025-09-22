package ifsp.edu.projeto.cortaai.barber;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = "/api/barbers", produces = MediaType.APPLICATION_JSON_VALUE)
public class BarberResource {

    private final BarberService barberService;

    public BarberResource(final BarberService barberService) {
        this.barberService = barberService;
    }

    @GetMapping
    public ResponseEntity<List<BarberDTO>> getAllBarbers() {
        return ResponseEntity.ok(barberService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberDTO> getBarber(@PathVariable(name = "id") final UUID id) {
        return ResponseEntity.ok(barberService.get(id));
    }

    @PostMapping
    @ApiResponse(responseCode = "201")
    public ResponseEntity<UUID> createBarber(@RequestBody @Valid final BarberDTO barberDTO) {
        final UUID createdId = barberService.create(barberDTO);
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
