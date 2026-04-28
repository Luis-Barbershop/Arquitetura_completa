package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints internos (inter-serviço).
 * Consumidos pelo payment-service (Dev 2).
 * NÃO expostos no API Gateway.
 */
@RestController
@RequestMapping(value = "/api/internal/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Internal - Appointments", description = "Endpoints internos consumidos pelo payment-service via Feign (NÃO expostos pelo Gateway)")
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Busca agendamento por ID (interno)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @Operation(summary = "Busca agendamentos da barbearia por período (interno)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agendamentos encontrados"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/barbershop/{barbershopId}/period")
    public ResponseEntity<List<AppointmentDTO>> getBarbershopAppointmentsByPeriod(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID barbershopId,
            @Parameter(description = "Início do período (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fim do período (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(appointmentService.getBarbershopAppointmentsByPeriod(barbershopId, from, to));
    }

    @Operation(summary = "Atualiza status de pagamento de um agendamento (interno)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status atualizado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) String bodyStatus) {
        appointmentService.updatePaymentStatus(id, status != null ? status : bodyStatus);
        return ResponseEntity.noContent().build();
    }
}
