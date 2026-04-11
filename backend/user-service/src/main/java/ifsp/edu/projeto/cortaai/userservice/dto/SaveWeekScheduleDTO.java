package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para salvar toda a agenda semanal do barbeiro de uma vez.
 * O frontend envia a semana inteira; o backend substitui todos os blocos anteriores.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveWeekScheduleDTO {

    @NotNull
    @Valid
    private List<DayScheduleDTO> schedule;
}
