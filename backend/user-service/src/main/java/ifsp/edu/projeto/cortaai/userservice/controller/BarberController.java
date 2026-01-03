package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CreateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
public class BarberController {

    private final BarberService barberService;

    @PostMapping("/signup")
    public ResponseEntity<BarberDTO> createBarber(@RequestBody @Valid CreateBarberDTO createBarberDTO) {
        BarberDTO createdBarber = barberService.createBarber(createBarberDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBarber);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginDTO loginDTO) {
        LoginResponseDTO loginResponse = barberService.login(loginDTO);
        return ResponseEntity.ok(loginResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BarberDTO> updateBarber(@PathVariable Long id, @RequestBody @Valid UpdateBarberDTO updateBarberDTO) {
        return ResponseEntity.ok(barberService.update(id, updateBarberDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BarberDTO> getBarberById(@PathVariable Long id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<BarberDTO>> getAllBarbers() {
        return ResponseEntity.ok(barberService.findAll());
    }
}