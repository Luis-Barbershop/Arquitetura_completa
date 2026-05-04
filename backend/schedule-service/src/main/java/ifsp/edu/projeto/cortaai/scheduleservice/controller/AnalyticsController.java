package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AgendaThermometerResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberSkillMatrixResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/appointments/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/agenda-thermometer")
    public ResponseEntity<List<AgendaThermometerResponseDTO>> getAgendaThermometer(
            @RequestParam String barbershopId) {
        return ResponseEntity.ok(analyticsService.getAgendaThermometer(barbershopId));
    }

    @GetMapping("/barber-skill-matrix")
    public ResponseEntity<List<BarberSkillMatrixResponseDTO>> getBarberSkillMatrix(
            @RequestParam String barbershopId) {
        return ResponseEntity.ok(analyticsService.getBarberSkillMatrix(barbershopId));
    }
}
