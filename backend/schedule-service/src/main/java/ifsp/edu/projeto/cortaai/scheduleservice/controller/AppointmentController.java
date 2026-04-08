package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Endpoints para gerenciamento de agendamentos, cancelamentos e consultas de agenda")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "Criar agendamento", description = "Cria um novo agendamento. Na arquitetura de microserviços, os dados do cliente e da barbearia/barbeiro são validados via comunicação inter-serviços (Feign Client) e salvos como snapshot (desnormalizados).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Agendamento criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Erro de validação ou horário indisponível",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro, barbearia ou atividade não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Horário já ocupado (conflito)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AppointmentDTO> createAppointment(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail,
            @Parameter(description = "Dados para criação do agendamento") @RequestBody @Valid CreateAppointmentDTO dto) {
        AppointmentDTO created = appointmentService.createAppointment(userEmail, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Buscar agendamento por ID", description = "Retorna os detalhes de um agendamento específico.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agendamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointment(
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @Operation(summary = "Listar meus agendamentos", description = "Retorna todos os agendamentos vinculados ao usuário logado, independentemente de ser um Customer ou Barber.")
    @GetMapping("/my-appointments")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointments(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(userEmail));
    }

    @Operation(summary = "Consultar agenda do barbeiro", description = "Retorna a agenda de um barbeiro específico. A consulta agora exige obrigatoriamente um filtro por data para evitar sobrecarga de dados.")
    @GetMapping("/barber/{barberId}")
    public ResponseEntity<List<AppointmentDTO>> getBarberSchedule(
            @Parameter(description = "UUID do barbeiro") @PathVariable UUID barberId,
            @Parameter(description = "Data do filtro (formato ISO: YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getBarberSchedule(barberId, date));
    }

    @Operation(summary = "Consultar agenda da barbearia", description = "Retorna todos os agendamentos de uma barbearia. Exige obrigatoriamente um filtro por data.")
    @GetMapping("/barbershop/{shopId}")
    public ResponseEntity<List<AppointmentDTO>> getBarbershopSchedule(
            @Parameter(description = "UUID da barbearia") @PathVariable UUID shopId,
            @Parameter(description = "Data do filtro (formato ISO: YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getBarbershopSchedule(shopId, date));
    }

    @Operation(summary = "Cancelar agendamento", description = "Cancela um agendamento existente de forma explícita e segura (substitui o antigo update genérico do monolito).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Agendamento cancelado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado inválido para cancelamento",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail,
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id) {
        appointmentService.cancelAppointment(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Concluir agendamento", description = "Marca um agendamento como concluído de forma explícita e segura.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Agendamento concluído"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado inválido para conclusão",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/conclude")
    public ResponseEntity<Void> concludeAppointment(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail,
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id) {
        appointmentService.concludeAppointment(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Completar agendamento", description = "Alias canônico para concluir um agendamento de forma explícita e segura.")
    @PutMapping("/{id}/complete")
    public ResponseEntity<Void> completeAppointment(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail,
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id) {
        appointmentService.concludeAppointment(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirmar agendamento", description = "Marca um agendamento pendente como confirmado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Agendamento confirmado"),
            @ApiResponse(responseCode = "404", description = "Agendamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Estado inválido para confirmação",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PutMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmAppointment(
            @Parameter(hidden = true) @RequestHeader("X-User-Email") String userEmail,
            @Parameter(description = "UUID do agendamento") @PathVariable UUID id) {
        appointmentService.confirmAppointment(userEmail, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consultar horários disponíveis", description = "Verifica os horários (slots) que ainda estão livres para agendamento com um barbeiro específico numa determinada data. O parâmetro 'duration' (em minutos) filtra apenas os blocos de tempo que cabem a duração total dos serviços selecionados.")
    @GetMapping("/availability")
    public ResponseEntity<List<TimeSlotDTO>> getAvailability(
            @Parameter(description = "UUID do barbeiro") @RequestParam UUID barberId,
            @Parameter(description = "Data para consulta (formato ISO: YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Duração total dos serviços em minutos (padrão: 30)") @RequestParam(defaultValue = "30") int duration) {
        return ResponseEntity.ok(appointmentService.getAvailability(barberId, date, duration));
    }

    @Operation(
            summary = "Agendamento manual pelo barbeiro (walk-in)",
            description = "Permite que um barbeiro crie um agendamento diretamente para um cliente presencial, " +
                    "sem exigir que o cliente possua conta no sistema e sem gerar fluxo de pagamento. " +
                    "O agendamento é registrado com status WALK_IN.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Agendamento walk-in criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou horário indisponível",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Barbeiro, barbearia ou atividade não encontrada",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Horário já ocupado (conflito)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/barber-booking")
    public ResponseEntity<AppointmentDTO> createManualBooking(
            @Parameter(hidden = true) @RequestHeader("X-User-UID") String firebaseUid,
            @Parameter(description = "Dados do agendamento manual") @RequestBody @Valid BarberManualBookingDTO dto) {
        AppointmentDTO created = appointmentService.createManualBooking(firebaseUid, dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
}