package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleAppointmentDTO(
        @NotNull LocalDateTime newStartTime
) {
}