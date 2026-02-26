package ifsp.edu.projeto.cortaai.schedule.service.impl;

import ifsp.edu.projeto.cortaai.schedule.dto.BarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateBarberWorkHoursDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.DailyAvailabilityDTO;
import ifsp.edu.projeto.cortaai.schedule.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.schedule.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.schedule.mapper.BarberWorkHoursMapper;
import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import ifsp.edu.projeto.cortaai.schedule.model.BarberWorkHours;
import ifsp.edu.projeto.cortaai.schedule.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.schedule.repository.BarberWorkHoursRepository;
import ifsp.edu.projeto.cortaai.schedule.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AvailabilityServiceImpl implements AvailabilityService {

    private final BarberWorkHoursRepository workHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final BarberWorkHoursMapper workHoursMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BarberWorkHoursDTO> getWorkHours(UUID barberId) {
        return workHoursMapper.toDTOList(workHoursRepository.findByBarberId(barberId));
    }

    @Override
    public BarberWorkHoursDTO setWorkHours(UUID barberId, UUID barbershopId, CreateBarberWorkHoursDTO dto) {
        // Check if work hours already exist for this day
        workHoursRepository.findByBarberIdAndDayOfWeek(barberId, dto.getDayOfWeek())
                .ifPresent(existing -> {
                    existing.setStartTime(dto.getStartTime());
                    existing.setEndTime(dto.getEndTime());
                    existing.setActive(true);
                    workHoursRepository.save(existing);
                });

        BarberWorkHours workHours = workHoursMapper.toEntity(dto);
        workHours.setBarberId(barberId);
        workHours.setBarbershopId(barbershopId);

        return workHoursMapper.toDTO(workHoursRepository.save(workHours));
    }

    @Override
    public void deleteWorkHours(Long id, UUID barberId) {
        BarberWorkHours workHours = workHoursRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Horário de trabalho", id));

        if (!workHours.getBarberId().equals(barberId)) {
            throw new ForbiddenException("Você não tem permissão para excluir este horário");
        }

        workHoursRepository.delete(workHours);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyAvailabilityDTO> getAvailability(UUID barberId, LocalDate startDate, LocalDate endDate, int slotMinutes) {
        List<DailyAvailabilityDTO> availability = new ArrayList<>();
        List<BarberWorkHours> workHours = workHoursRepository.findByBarberIdAndIsActive(barberId, true);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            
            // Find work hours for this day
            BarberWorkHours dayWorkHours = workHours.stream()
                    .filter(wh -> wh.getDayOfWeek() == dayOfWeek)
                    .findFirst()
                    .orElse(null);

            if (dayWorkHours == null) {
                // No work hours for this day
                availability.add(new DailyAvailabilityDTO(date.toString(), List.of()));
                continue;
            }

            List<DailyAvailabilityDTO.TimeSlot> slots = generateTimeSlots(
                    barberId, date, dayWorkHours.getStartTime(), dayWorkHours.getEndTime(), slotMinutes);
            
            availability.add(new DailyAvailabilityDTO(date.toString(), slots));
        }

        return availability;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTimeSlotAvailable(UUID barberId, OffsetDateTime startTime, OffsetDateTime endTime) {
        // Check if there are conflicting appointments
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(barberId, startTime, endTime);
        return conflicts.isEmpty();
    }

    private List<DailyAvailabilityDTO.TimeSlot> generateTimeSlots(
            UUID barberId, LocalDate date, LocalTime start, LocalTime end, int slotMinutes) {
        
        List<DailyAvailabilityDTO.TimeSlot> slots = new ArrayList<>();

        OffsetDateTime dayStart = date.atTime(start).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime dayEnd = date.atTime(end).atZone(ZoneId.systemDefault()).toOffsetDateTime();

        // Get existing appointments for this day
        List<Appointment> appointments = appointmentRepository.findByBarberIdAndTimeRange(
                barberId, dayStart, dayEnd);

        LocalTime current = start;
        while (current.plusMinutes(slotMinutes).isBefore(end) || current.plusMinutes(slotMinutes).equals(end)) {
            LocalTime slotEnd = current.plusMinutes(slotMinutes);
            
            OffsetDateTime slotStartDT = date.atTime(current).atZone(ZoneId.systemDefault()).toOffsetDateTime();
            OffsetDateTime slotEndDT = date.atTime(slotEnd).atZone(ZoneId.systemDefault()).toOffsetDateTime();

            boolean available = appointments.stream()
                    .noneMatch(a -> a.getStartTime().isBefore(slotEndDT) && a.getEndTime().isAfter(slotStartDT));

            slots.add(new DailyAvailabilityDTO.TimeSlot(current, slotEnd, available));
            current = slotEnd;
        }

        return slots;
    }
}
