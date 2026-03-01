package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AppointmentService;
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
@RequestMapping(value = "/api/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentDTO> createAppointment(
            Principal principal,
            @RequestBody @Valid CreateAppointmentDTO dto) {
        AppointmentDTO created = appointmentService.createAppointment(principal.getName(), dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/my-appointments")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointments(Principal principal) {
        return ResponseEntity.ok(appointmentService.getMyAppointments(principal.getName()));
    }

    @GetMapping("/barber/{barberId}")
    public ResponseEntity<List<AppointmentDTO>> getBarberSchedule(
            @PathVariable UUID barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getBarberSchedule(barberId, date));
    }

    @GetMapping("/barbershop/{shopId}")
    public ResponseEntity<List<AppointmentDTO>> getBarbershopSchedule(
            @PathVariable UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getBarbershopSchedule(shopId, date));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelAppointment(Principal principal, @PathVariable UUID id) {
        appointmentService.cancelAppointment(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/conclude")
    public ResponseEntity<Void> concludeAppointment(Principal principal, @PathVariable UUID id) {
        appointmentService.concludeAppointment(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmAppointment(Principal principal, @PathVariable UUID id) {
        appointmentService.confirmAppointment(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/availability")
    public ResponseEntity<List<TimeSlotDTO>> getAvailability(
            @RequestParam UUID barberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailability(barberId, date));
    }
}

