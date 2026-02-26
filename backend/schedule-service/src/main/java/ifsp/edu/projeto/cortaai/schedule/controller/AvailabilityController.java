package ifsp.edu.projeto.cortaai.schedule.controller;

import ifsp.edu.projeto.cortaai.schedule.dto.BarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateBarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.DailyAvailabilityDTO;
import ifsp.edu.projeto.cortaai.schedule.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping(value = "/api/availability", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Endpoints para gerenciamento de disponibilidade")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/barber/{barberId}/work-hours")
    @Operation(summary = "Lista horários de trabalho de um barbeiro")
    public ResponseEntity<List<BarberWorkHoursDTO>> getWorkHours(@PathVariable UUID barberId) {
        return ResponseEntity.ok(availabilityService.getWorkHours(barberId));
    }

    @PostMapping("/barber/work-hours")
    @Operation(summary = "Define horário de trabalho para um barbeiro")
    @ApiResponse(responseCode = "201")
    public ResponseEntity<BarberWorkHoursDTO> setWorkHours(
            @RequestHeader("X-User-Id") UUID barberId,
            @RequestHeader("X-Barbershop-Id") UUID barbershopId,
            @RequestBody @Valid CreateBarberWorkHoursDTO dto) {
        BarberWorkHoursDTO created = availabilityService.setWorkHours(barberId, barbershopId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/barber/work-hours/{id}")
    @Operation(summary = "Remove horário de trabalho")
    @ApiResponse(responseCode = "204")
    public ResponseEntity<Void> deleteWorkHours(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") UUID barberId) {
        availabilityService.deleteWorkHours(id, barberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/barber/{barberId}/slots")
    @Operation(summary = "Obtém disponibilidade de um barbeiro por período")
    public ResponseEntity<List<DailyAvailabilityDTO>> getAvailability(
            @PathVariable UUID barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "30") int slotMinutes) {
        return ResponseEntity.ok(availabilityService.getAvailability(barberId, startDate, endDate, slotMinutes));
    }
}
