package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CreateBarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.service.BarberBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/appointments/barber-blocks", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class BarberBlockController {

    private final BarberBlockService barberBlockService;

    @PostMapping
    public ResponseEntity<BarberBlockDTO> createBlock(
            Principal principal,
            @RequestBody @Valid CreateBarberBlockDTO dto) {
        BarberBlockDTO created = barberBlockService.createBlock(principal.getName(), dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BarberBlockDTO>> getBlocks(
            @RequestParam UUID barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(barberBlockService.getBlocks(barberId, date));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBlock(Principal principal, @PathVariable UUID id) {
        barberBlockService.deleteBlock(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }
}

