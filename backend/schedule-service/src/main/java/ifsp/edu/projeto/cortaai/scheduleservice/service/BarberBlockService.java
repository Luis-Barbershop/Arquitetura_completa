package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.*;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ConflictException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.BarberBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BarberBlockService {

    private final BarberBlockRepository barberBlockRepository;
    private final UserServiceClient userServiceClient;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentService appointmentService;

    public BarberBlockDTO createBlock(String callerFirebaseUid, CreateBarberBlockDTO dto) {
        // Verificar que caller é o próprio barbeiro
        UserInfoDTO caller = userServiceClient.getUserByFirebaseUid(callerFirebaseUid);
        if (!caller.getId().equals(dto.getBarberId())) {
            throw new NotFoundException("Você só pode criar bloqueios para sua própria agenda.");
        }

        if (dto.getEndTime().isBefore(dto.getStartTime()) || dto.getEndTime().isEqual(dto.getStartTime())) {
            throw new IllegalArgumentException("O horário final deve ser posterior ao horário inicial.");
        }

        // Verificar sobreposição com bloqueios existentes
        boolean overlaps = barberBlockRepository
                .existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        dto.getBarberId(), dto.getEndTime(), dto.getStartTime());
        if (overlaps) {
            throw new ConflictException("Já existe um bloqueio que conflita com este período.");
        }

        BarberBlock block = BarberBlock.builder()
                .barberId(dto.getBarberId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .reason(dto.getReason())
                .build();

        BarberBlock saved = barberBlockRepository.save(block);

        int cancelledAppointments = appointmentService.cancelAppointmentsOverlappingBarberBlock(
            dto.getBarberId(), dto.getStartTime(), dto.getEndTime());
        if (cancelledAppointments > 0) {
            log.info("Bloqueio criado para barberId={} com {} agendamentos cancelados no intervalo [{} - {}]",
                dto.getBarberId(), cancelledAppointments, dto.getStartTime(), dto.getEndTime());
        }

        BarberBlockDTO response = appointmentMapper.toBlockDTO(saved);
        response.setCancelledAppointmentsCount(cancelledAppointments);
        return response;
    }

    @Transactional(readOnly = true)
    public List<BarberBlockDTO> getBlocks(UUID barberId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);

        List<BarberBlock> blocks = barberBlockRepository
                .findByBarberIdAndStartTimeBetween(barberId, dayStart, dayEnd);

        return appointmentMapper.toBlockDTOList(blocks);
    }

    public void deleteBlock(String callerFirebaseUid, UUID blockId) {
        BarberBlock block = barberBlockRepository.findById(blockId)
                .orElseThrow(() -> new NotFoundException("Bloqueio não encontrado."));

        // Verificar que caller é o dono do bloqueio
        UserInfoDTO caller = userServiceClient.getUserByFirebaseUid(callerFirebaseUid);
        if (!caller.getId().equals(block.getBarberId())) {
            throw new NotFoundException("Você só pode remover bloqueios da sua própria agenda.");
        }

        barberBlockRepository.delete(block);
    }
}
