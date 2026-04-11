package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.*;
import ifsp.edu.projeto.cortaai.userservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import ifsp.edu.projeto.cortaai.userservice.service.BarberWorkScheduleService;
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
    private final BarberWorkScheduleService workScheduleService;

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

    @Operation(
            summary = "Lista os IDs das atividades atribuídas a um barbeiro (público)",
            description = "Retorna o conjunto de UUIDs das atividades que o barbeiro executa. " +
                          "Usado pelo cliente na tela de agendamento para filtrar quais serviços cada barbeiro oferece."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Barbeiro não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}/activities")
    public ResponseEntity<Set<UUID>> getBarberActivities(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id) {
        return ResponseEntity.ok(barberService.getAssignedActivityIdsById(id));
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

    // ========== AGENDA SEMANAL (BLOCOS DE HORÁRIO) ==========

    @Operation(
            summary = "Retorna a agenda semanal do barbeiro autenticado",
            description = "Lista todos os blocos de horário configurados por dia da semana."
    )
    @GetMapping("/me/work-schedule")
    public ResponseEntity<List<DayScheduleDTO>> getMyWorkSchedule(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid) {
        return ResponseEntity.ok(workScheduleService.getSchedule(firebaseUid));
    }

    @Operation(
            summary = "Salva (substitui) toda a agenda semanal do barbeiro autenticado",
            description = "Recebe os dias da semana com seus blocos de horário e substitui completamente a agenda anterior. " +
                          "Exemplo: Segunda 09:00–12:00 e 13:00–18:00, Terça 08:00–17:00."
    )
    @PutMapping("/me/work-schedule")
    public ResponseEntity<List<DayScheduleDTO>> saveMyWorkSchedule(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @RequestBody @Valid SaveWeekScheduleDTO dto) {
        return ResponseEntity.ok(workScheduleService.saveSchedule(firebaseUid, dto));
    }

    @Operation(
            summary = "Retorna a agenda semanal de um barbeiro por ID (público/inter-serviço)",
            description = "Usado pelo schedule-service para gerar slots disponíveis."
    )
    @GetMapping("/{id}/work-schedule")
    public ResponseEntity<List<DayScheduleDTO>> getBarberWorkSchedule(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID id) {
        return ResponseEntity.ok(workScheduleService.getScheduleByBarberId(id));
    }
}
