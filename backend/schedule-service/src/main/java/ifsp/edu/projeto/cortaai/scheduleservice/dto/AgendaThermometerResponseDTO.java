package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import java.time.LocalDate;

public record AgendaThermometerResponseDTO(
        LocalDate agendaDate,
        String barbershopId,
        Long totalAppointments,
        Long activeAppointments,
        Long walkinAppointments,
        Long pendingAppointments,
        Long completedAppointments,
        Long lostAppointments
) {}
