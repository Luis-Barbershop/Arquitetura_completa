package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Blocos de horário para um dia da semana (inter-serviço).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayScheduleDTO {
    private DayOfWeek dayOfWeek;
    private List<WorkBlockDTO> blocks;
}
