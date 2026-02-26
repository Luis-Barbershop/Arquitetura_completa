package ifsp.edu.projeto.cortaai.schedule.controller;

import ifsp.edu.projeto.cortaai.schedule.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.UpdateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Endpoints para gerenciamento de agendamentos")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Lista todos os agendamentos")
    public ResponseEntity<List<AppointmentDTO>> findAll() {
        return ResponseEntity.ok(appointmentService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca agendamento por ID")
    public ResponseEntity<AppointmentDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @GetMapping("/barbershop/{barbershopId}")
    @Operation(summary = "Lista agendamentos de uma barbearia")
    public ResponseEntity<List<AppointmentDTO>> findByBarbershop(@PathVariable UUID barbershopId) {
        return ResponseEntity.ok(appointmentService.findByBarbershopId(barbershopId));
    }

    @GetMapping("/barber/{barberId}")
    @Operation(summary = "Lista agendamentos de um barbeiro")
    public ResponseEntity<List<AppointmentDTO>> findByBarber(@PathVariable UUID barberId) {
        return ResponseEntity.ok(appointmentService.findByBarberId(barberId));
    }

    @GetMapping("/customer/me")
    @Operation(summary = "Lista agendamentos do cliente logado")
    public ResponseEntity<List<AppointmentDTO>> findByCustomer(
            @RequestHeader("X-User-Id") UUID customerId) {
        return ResponseEntity.ok(appointmentService.findByCustomerId(customerId));
    }

    @GetMapping("/customer/me/upcoming")
    @Operation(summary = "Lista próximos agendamentos do cliente")
    public ResponseEntity<List<AppointmentDTO>> findUpcoming(
            @RequestHeader("X-User-Id") UUID customerId) {
        return ResponseEntity.ok(appointmentService.findUpcomingByCustomerId(customerId));
    }

    @PostMapping
    @Operation(summary = "Cria um novo agendamento")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<AppointmentDTO> create(
            @RequestHeader("X-User-Id") UUID customerId,
            @RequestBody @Valid CreateAppointmentDTO dto) {
        AppointmentDTO created = appointmentService.create(dto, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um agendamento")
    public ResponseEntity<AppointmentDTO> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestBody @Valid UpdateAppointmentDTO dto) {
        return ResponseEntity.ok(appointmentService.update(id, dto, requesterId));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancela um agendamento")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") UUID requesterId) {
        appointmentService.cancel(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/conclude")
    @Operation(summary = "Conclui um agendamento")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> conclude(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") UUID requesterId) {
        appointmentService.conclude(id, requesterId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um agendamento")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") UUID requesterId) {
        appointmentService.delete(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}
