package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CreateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.LoginResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
@Tag(name = "Barbers", description = "Endpoints para gerenciamento de barbeiros e autenticação")
public class BarberController {

    private final BarberService barberService;

    @Operation(summary = "Registra um novo barbeiro", description = "Cria a conta de um novo barbeiro no sistema.")
    @PostMapping("/register")
    public ResponseEntity<BarberDTO> createBarber(
            @Parameter(description = "Dados de criação do barbeiro") @RequestBody @Valid CreateBarberDTO createBarberDTO) {
        BarberDTO createdBarber = barberService.createBarber(createBarberDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBarber);
    }

    @Operation(summary = "Realiza o login do barbeiro", description = "Autentica o barbeiro utilizando email e senha e retorna um token JWT.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Parameter(description = "Credenciais do barbeiro") @RequestBody @Valid LoginDTO loginDTO) {
        LoginResponseDTO loginResponse = barberService.login(loginDTO);
        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Atualiza o perfil do barbeiro", description = "Atualiza as informações de perfil de um barbeiro específico.")
    @PutMapping("/{id}")
    public ResponseEntity<BarberDTO> updateBarber(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id,
            @Parameter(description = "Novos dados do barbeiro") @RequestBody @Valid UpdateBarberDTO updateBarberDTO) {
        return ResponseEntity.ok(barberService.update(id, updateBarberDTO));
    }

    @Operation(summary = "Busca um barbeiro por ID", description = "Retorna os detalhes de um barbeiro específico.")
    @GetMapping("/{id}")
    public ResponseEntity<BarberDTO> getBarberById(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @Operation(summary = "Lista todos os barbeiros", description = "Retorna uma lista com todos os barbeiros cadastrados no sistema.")
    @GetMapping
    public ResponseEntity<List<BarberDTO>> getAllBarbers() {
        return ResponseEntity.ok(barberService.findAll());
    }

    @Operation(summary = "Lista barbeiros de uma barbearia", description = "Retorna todos os barbeiros associados a um determinado ID de barbearia.")
    @GetMapping("/barbershop/{barbershopId}")
    public ResponseEntity<List<BarberDTO>> getBarbersByBarbershop(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID barbershopId) {
        return ResponseEntity.ok(barberService.findByBarbershopId(barbershopId));
    }
}