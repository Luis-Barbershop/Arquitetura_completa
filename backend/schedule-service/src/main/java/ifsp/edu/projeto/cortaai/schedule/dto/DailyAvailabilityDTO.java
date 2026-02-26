package ifsp.edu.projeto.cortaai.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyAvailabilityDTO {
    private String date;
    private List<TimeSlot> availableSlots;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
        private boolean available;
    }
}
