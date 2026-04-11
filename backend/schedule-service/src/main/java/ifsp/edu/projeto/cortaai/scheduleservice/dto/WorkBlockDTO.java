package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Bloco de horário de trabalho (inter-serviço).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkBlockDTO {
    private LocalTime startTime;
    private LocalTime endTime;
}
