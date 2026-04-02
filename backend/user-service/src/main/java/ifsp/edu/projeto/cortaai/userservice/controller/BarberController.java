package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.AssignActivitiesDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoints de gerenciamento de perfil de barbeiros.
 *
 * <p>Registro e login foram movidos para {@link AuthController} ({@code /api/auth/verify}).
 */
@RestController
@RequestMapping("/api/barbers")
@RequiredArgsConstructor
@Tag(name = "Barbers", description = "Endpoints de perfil e listagem de barbeiros")
public class BarberController {

    private final BarberService barberService;

    @Operation(summary = "Atualiza o perfil de um barbeiro",
               description = "Atualização parcial (patch-like). Envie somente os campos que deseja alterar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<BarberDTO> updateBarber(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id,
            @RequestBody @Valid UpdateBarberDTO updateBarberDTO) {
        return ResponseEntity.ok(barberService.update(id, updateBarberDTO));
    }

    @Operation(summary = "Busca um barbeiro por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Barbeiro encontrado"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BarberDTO> getBarberById(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id) {
        return ResponseEntity.ok(barberService.findById(id));
    }

    @Operation(summary = "Lista todos os barbeiros")
    @GetMapping
    public ResponseEntity<List<BarberDTO>> getAllBarbers() {
        return ResponseEntity.ok(barberService.findAll());
    }

    @Operation(summary = "Lista barbeiros de uma barbearia")
    @GetMapping("/barbershop/{barbershopId}")
    public ResponseEntity<List<BarberDTO>> getBarbersByBarbershop(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID barbershopId) {
        return ResponseEntity.ok(barberService.findByBarbershopId(barbershopId));
    }

    // ========== HABILIDADES DO BARBEIRO AUTENTICADO ==========

    @Operation(
            summary = "Lista os IDs das atividades atribuídas ao barbeiro autenticado",
            description = "Retorna o conjunto de UUIDs das atividades que o barbeiro marcou como suas habilidades."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/me/my-activities")
    public ResponseEntity<Set<UUID>> getMyActivities(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        return ResponseEntity.ok(barberService.getAssignedActivityIds(firebaseUid));
    }

    @Operation(
            summary = "Atribui (substitui) as atividades do barbeiro autenticado",
            description = "Recebe uma lista de UUIDs de atividades e substitui completamente as habilidades registradas para o barbeiro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habilidades atualizadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/me/assign-activities")
    public ResponseEntity<Set<UUID>> assignActivities(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestBody @Valid AssignActivitiesDTO dto) {
        return ResponseEntity.ok(barberService.assignActivities(firebaseUid, dto));
    }
}
