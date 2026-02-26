package ifsp.edu.projeto.cortaai.schedule.service;

import ifsp.edu.projeto.cortaai.schedule.dto.BarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateBarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.DailyAvailabilityDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AvailabilityService {

    List<BarberWorkHoursDTO> getWorkHours(UUID barberId);

    BarberWorkHoursDTO setWorkHours(UUID barberId, UUID barbershopId, CreateBarberWorkHoursDTO dto);

    void deleteWorkHours(Long id, UUID barberId);

    List<DailyAvailabilityDTO> getAvailability(UUID barberId, LocalDate startDate, LocalDate endDate, int slotMinutes);

    boolean isTimeSlotAvailable(UUID barberId, java.time.OffsetDateTime startTime, java.time.OffsetDateTime endTime);
}
