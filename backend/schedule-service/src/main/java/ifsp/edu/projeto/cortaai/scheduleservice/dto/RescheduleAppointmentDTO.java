package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record RescheduleAppointmentDTO(
        @NotNull LocalDateTime newStartTime,
        UUID barberId  // nullable — null mantém o barbeiro original (backward-compatible)
) {
}