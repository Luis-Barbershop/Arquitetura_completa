package ifsp.edu.projeto.cortaai.schedule.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
public class BarberWorkHoursDTO {
    private Long id;
    private UUID barberId;
    private UUID barbershopId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isActive;
}
