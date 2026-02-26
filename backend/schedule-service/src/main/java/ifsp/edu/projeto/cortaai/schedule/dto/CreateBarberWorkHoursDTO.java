package ifsp.edu.projeto.cortaai.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class CreateBarberWorkHoursDTO {

    @NotNull(message = "Dia da semana é obrigatório")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Horário de início é obrigatório")
    private LocalTime startTime;

    @NotNull(message = "Horário de término é obrigatório")
    private LocalTime endTime;
}
