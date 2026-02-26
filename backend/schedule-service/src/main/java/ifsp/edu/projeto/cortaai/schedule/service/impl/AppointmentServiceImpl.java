package ifsp.edu.projeto.cortaai.schedule.service.impl;

import ifsp.edu.projeto.cortaai.schedule.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.CreateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.dto.UpdateAppointmentDTO;
import ifsp.edu.projeto.cortaai.schedule.exception.ConflictException;
import ifsp.edu.projeto.cortaai.schedule.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.schedule.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.schedule.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.schedule.model.Appointment;
import ifsp.edu.projeto.cortaai.schedule.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.schedule.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.schedule.service.AppointmentService;
import ifsp.edu.projeto.cortaai.schedule.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;
    private final AvailabilityService availabilityService;

    private static final int DEFAULT_DURATION_MINUTES = 30;

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> findAll() {
        return appointmentMapper.toDTOList(appointmentRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentDTO findById(Long id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Agendamento", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByBarbershopId(UUID barbershopId) {
        return appointmentMapper.toDTOList(appointmentRepository.findByBarbershopId(barbershopId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByBarberId(UUID barberId) {
        return appointmentMapper.toDTOList(appointmentRepository.findByBarberId(barberId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> findByCustomerId(UUID customerId) {
        return appointmentMapper.toDTOList(appointmentRepository.findByCustomerId(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> findUpcomingByCustomerId(UUID customerId) {
        return appointmentMapper.toDTOList(
                appointmentRepository.findUpcomingAppointments(customerId, OffsetDateTime.now()));
    }

    @Override
    public AppointmentDTO create(CreateAppointmentDTO dto, UUID customerId) {
        OffsetDateTime endTime = dto.getStartTime().plusMinutes(DEFAULT_DURATION_MINUTES);

        // Check for conflicts
        if (!availabilityService.isTimeSlotAvailable(dto.getBarberId(), dto.getStartTime(), endTime)) {
            throw new ConflictException("Horário não disponível para este barbeiro");
        }

        Appointment appointment = appointmentMapper.toEntity(dto);
        appointment.setCustomerId(customerId);
        appointment.setEndTime(endTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setActivityIds(new HashSet<>(dto.getActivityIds()));

        return appointmentMapper.toDTO(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentDTO update(Long id, UpdateAppointmentDTO dto, UUID requesterId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento", id));

        validateCustomerAccess(appointment, requesterId);

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new ConflictException("Não é possível atualizar um agendamento que não está agendado");
        }

        if (dto.getStartTime() != null) {
            OffsetDateTime newEndTime = dto.getStartTime().plusMinutes(DEFAULT_DURATION_MINUTES);
            UUID barberId = dto.getBarberId() != null ? dto.getBarberId() : appointment.getBarberId();
            
            if (!availabilityService.isTimeSlotAvailable(barberId, dto.getStartTime(), newEndTime)) {
                throw new ConflictException("Horário não disponível para este barbeiro");
            }
            appointment.setEndTime(newEndTime);
        }

        appointmentMapper.updateEntityFromDTO(appointment, dto);

        if (dto.getActivityIds() != null) {
            appointment.setActivityIds(new HashSet<>(dto.getActivityIds()));
        }

        return appointmentMapper.toDTO(appointmentRepository.save(appointment));
    }

    @Override
    public void cancel(Long id, UUID requesterId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento", id));

        // Both customer and barbershop owner can cancel
        if (!appointment.getCustomerId().equals(requesterId) && 
            !isBarbershopOwner(appointment.getBarbershopId(), requesterId)) {
            throw new ForbiddenException("Você não tem permissão para cancelar este agendamento");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED ||
            appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new ConflictException("Não é possível cancelar este agendamento");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @Override
    public void conclude(Long id, UUID requesterId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento", id));

        // Only the barber can conclude
        if (!appointment.getBarberId().equals(requesterId)) {
            throw new ForbiddenException("Apenas o barbeiro pode concluir este agendamento");
        }

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED &&
            appointment.getStatus() != AppointmentStatus.CONFIRMED &&
            appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
            throw new ConflictException("Não é possível concluir este agendamento");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }

    @Override
    public void delete(Long id, UUID requesterId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Agendamento", id));

        // Only barbershop owner can delete
        if (!isBarbershopOwner(appointment.getBarbershopId(), requesterId)) {
            throw new ForbiddenException("Apenas o proprietário pode excluir agendamentos");
        }

        appointmentRepository.delete(appointment);
    }

    private void validateCustomerAccess(Appointment appointment, UUID customerId) {
        if (!appointment.getCustomerId().equals(customerId)) {
            throw new ForbiddenException("Você não tem permissão para modificar este agendamento");
        }
    }

    private boolean isBarbershopOwner(UUID barbershopId, UUID userId) {
        // In a real implementation, this would call barbershop-service
        // For now, we'll return true to allow the operation
        // This should be implemented with Feign client
        return true;
    }
}
