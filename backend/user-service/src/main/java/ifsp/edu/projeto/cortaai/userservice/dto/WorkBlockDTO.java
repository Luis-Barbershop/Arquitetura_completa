package ifsp.edu.projeto.cortaai.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Um bloco de horário individual (ex: 09:00–12:00).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkBlockDTO {

    @NotNull(message = "Horário de início é obrigatório")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Horário de término é obrigatório")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
}
