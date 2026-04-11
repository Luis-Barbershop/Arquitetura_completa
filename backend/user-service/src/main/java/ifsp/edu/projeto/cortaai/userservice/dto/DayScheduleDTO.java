package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Blocos de horário para um dia da semana específico.
 * Exemplo: { dayOfWeek: "MONDAY", blocks: [{startTime:"09:00", endTime:"12:00"}, {startTime:"13:00", endTime:"18:00"}] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayScheduleDTO {

    @NotNull(message = "Dia da semana é obrigatório")
    private DayOfWeek dayOfWeek;

    @NotNull
    @Valid
    private List<WorkBlockDTO> blocks;
}
