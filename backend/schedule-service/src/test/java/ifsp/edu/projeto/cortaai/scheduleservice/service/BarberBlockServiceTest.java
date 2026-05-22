package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CreateBarberBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ConflictException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.scheduleservice.model.BarberBlock;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.BarberBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarberBlockServiceTest {

    @Mock
    private BarberBlockRepository barberBlockRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private BarberBlockService service;

    @Test
    void shouldCreateBlockForAuthenticatedBarber() {
        UUID barberId = UUID.randomUUID();
        CreateBarberBlockDTO request = blockRequest(barberId,
                LocalDateTime.of(2026, 5, 21, 9, 0),
                LocalDateTime.of(2026, 5, 21, 10, 0),
                "Consulta");
        BarberBlock saved = BarberBlock.builder()
                .id(UUID.randomUUID())
                .barberId(barberId)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason("Consulta")
                .build();
        BarberBlockDTO response = new BarberBlockDTO();
        response.setId(saved.getId());
        response.setBarberId(barberId);

        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user(barberId));
        when(barberBlockRepository.existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
                barberId, request.getEndTime(), request.getStartTime())).thenReturn(false);
        when(barberBlockRepository.save(any(BarberBlock.class))).thenReturn(saved);
        when(appointmentMapper.toBlockDTO(saved)).thenReturn(response);

        BarberBlockDTO result = service.createBlock("firebase-1", request);

        assertThat(result).isEqualTo(response);
        ArgumentCaptor<BarberBlock> captor = ArgumentCaptor.forClass(BarberBlock.class);
        verify(barberBlockRepository).save(captor.capture());
        assertThat(captor.getValue().getBarberId()).isEqualTo(barberId);
        assertThat(captor.getValue().getReason()).isEqualTo("Consulta");
    }

    @Test
    void shouldRejectBlockForAnotherBarber() {
        UUID callerId = UUID.randomUUID();
        UUID anotherBarberId = UUID.randomUUID();

        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user(callerId));

        assertThatThrownBy(() -> service.createBlock("firebase-1", blockRequest(
                anotherBarberId,
                LocalDateTime.of(2026, 5, 21, 9, 0),
                LocalDateTime.of(2026, 5, 21, 10, 0),
                "Outro barbeiro")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Você só pode criar bloqueios para sua própria agenda.");

        verify(barberBlockRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        UUID barberId = UUID.randomUUID();
        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user(barberId));

        assertThatThrownBy(() -> service.createBlock("firebase-1", blockRequest(
                barberId,
                LocalDateTime.of(2026, 5, 21, 10, 0),
                LocalDateTime.of(2026, 5, 21, 10, 0),
                "Sem duração")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O horário final deve ser posterior ao horário inicial.");

        verify(barberBlockRepository, never()).save(any());
    }

    @Test
    void shouldRejectOverlappingBlock() {
        UUID barberId = UUID.randomUUID();
        CreateBarberBlockDTO request = blockRequest(barberId,
                LocalDateTime.of(2026, 5, 21, 9, 0),
                LocalDateTime.of(2026, 5, 21, 10, 0),
                "Conflito");

        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user(barberId));
        when(barberBlockRepository.existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(
                barberId, request.getEndTime(), request.getStartTime())).thenReturn(true);

        assertThatThrownBy(() -> service.createBlock("firebase-1", request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Já existe um bloqueio que conflita com este período.");

        verify(barberBlockRepository, never()).save(any());
    }

    @Test
    void shouldListBlocksForDayBoundaries() {
        UUID barberId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 5, 21);
        List<BarberBlock> blocks = List.of(BarberBlock.builder().barberId(barberId).build());
        List<BarberBlockDTO> mapped = List.of(new BarberBlockDTO());

        when(barberBlockRepository.findByBarberIdAndStartTimeBetween(
                eq(barberId),
                eq(LocalDateTime.of(2026, 5, 21, 0, 0)),
                eq(LocalDateTime.of(2026, 5, 21, 23, 59, 59))))
                .thenReturn(blocks);
        when(appointmentMapper.toBlockDTOList(blocks)).thenReturn(mapped);

        assertThat(service.getBlocks(barberId, date)).isEqualTo(mapped);
    }

    @Test
    void shouldDeleteOwnBlock() {
        UUID barberId = UUID.randomUUID();
        UUID blockId = UUID.randomUUID();
        BarberBlock block = BarberBlock.builder().id(blockId).barberId(barberId).build();

        when(barberBlockRepository.findById(blockId)).thenReturn(Optional.of(block));
        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user(barberId));

        service.deleteBlock("firebase-1", blockId);

        verify(barberBlockRepository).delete(block);
    }

    @Test
    void shouldRejectDeletingAnotherBarberBlock() {
        UUID blockId = UUID.randomUUID();
        BarberBlock block = BarberBlock.builder().id(blockId).barberId(UUID.randomUUID()).build();

        when(barberBlockRepository.findById(blockId)).thenReturn(Optional.of(block));
        when(userServiceClient.getUserByFirebaseUid("firebase-1")).thenReturn(user(UUID.randomUUID()));

        assertThatThrownBy(() -> service.deleteBlock("firebase-1", blockId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Você só pode remover bloqueios da sua própria agenda.");

        verify(barberBlockRepository, never()).delete(any());
    }

    private CreateBarberBlockDTO blockRequest(UUID barberId, LocalDateTime start, LocalDateTime end, String reason) {
        CreateBarberBlockDTO request = new CreateBarberBlockDTO();
        request.setBarberId(barberId);
        request.setStartTime(start);
        request.setEndTime(end);
        request.setReason(reason);
        return request;
    }

    private UserInfoDTO user(UUID id) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        return user;
    }
}
