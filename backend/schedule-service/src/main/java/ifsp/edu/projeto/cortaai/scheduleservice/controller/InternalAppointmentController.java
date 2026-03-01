package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoints internos (inter-serviço).
 * Consumidos pelo payment-service (Dev 2).
 * NÃO expostos no API Gateway.
 */
@RestController
@RequestMapping(value = "/api/internal/appointments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InternalAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable UUID id,
            @RequestBody String status) {
        appointmentService.updatePaymentStatus(id, status);
        return ResponseEntity.noContent().build();
    }
}

