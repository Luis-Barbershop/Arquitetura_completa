package ifsp.edu.projeto.cortaai.schedule.service;

import ifsp.edu.projeto.cortaai.schedule.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.UpdateAppointmentDTO;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    List<AppointmentDTO> findAll();

    AppointmentDTO findById(Long id);

    List<AppointmentDTO> findByBarbershopId(UUID barbershopId);

    List<AppointmentDTO> findByBarberId(UUID barberId);

    List<AppointmentDTO> findByCustomerId(UUID customerId);

    List<AppointmentDTO> findUpcomingByCustomerId(UUID customerId);

    AppointmentDTO create(CreateAppointmentDTO dto, UUID customerId);

    AppointmentDTO update(Long id, UpdateAppointmentDTO dto, UUID requesterId);

    void cancel(Long id, UUID requesterId);

    void conclude(Long id, UUID requesterId);

    void delete(Long id, UUID requesterId);
}
