package ifsp.edu.projeto.cortaai.userservice.service;

import ifsp.edu.projeto.cortaai.userservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.SaveWeekScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.WorkBlockDTO;
import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.BarberWorkBlock;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberWorkBlockRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarberWorkScheduleServiceTest {

    @Mock
    private BarberRepository barberRepository;

    @Mock
    private BarberWorkBlockRepository workBlockRepository;

    @InjectMocks
    private BarberWorkScheduleService service;

    @Test
    void shouldGroupScheduleByDayInRepositoryOrder() {
        UUID barberId = UUID.randomUUID();
        when(workBlockRepository.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(barberId))
                .thenReturn(List.of(
                        block(barberId, DayOfWeek.MONDAY, "09:00", "12:00"),
                        block(barberId, DayOfWeek.MONDAY, "13:00", "18:00"),
                        block(barberId, DayOfWeek.TUESDAY, "10:00", "16:00")
                ));

        List<DayScheduleDTO> schedule = service.getScheduleByBarberId(barberId);

        assertThat(schedule).hasSize(2);
        assertThat(schedule.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedule.get(0).getBlocks()).extracting(WorkBlockDTO::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(13, 0));
        assertThat(schedule.get(1).getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
    }

    @Test
    void shouldReplaceScheduleAndUpdateLegacyWorkHours() {
        UUID barberId = UUID.randomUUID();
        Barber barber = Barber.builder().id(barberId).firebaseUid("firebase-1").build();
        SaveWeekScheduleDTO request = new SaveWeekScheduleDTO(List.of(
                new DayScheduleDTO(DayOfWeek.MONDAY, List.of(
                        new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new WorkBlockDTO(LocalTime.of(14, 0), LocalTime.of(18, 0))
                )),
                new DayScheduleDTO(DayOfWeek.WEDNESDAY, List.of(
                        new WorkBlockDTO(LocalTime.of(8, 30), LocalTime.of(16, 30))
                ))
        ));

        when(barberRepository.findByFirebaseUid("firebase-1")).thenReturn(Optional.of(barber));
        when(workBlockRepository.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(barberId))
                .thenReturn(List.of(block(barberId, DayOfWeek.MONDAY, "09:00", "12:00")));

        List<DayScheduleDTO> result = service.saveSchedule("firebase-1", request);

        verify(workBlockRepository).deleteByBarberId(barberId);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BarberWorkBlock>> blocksCaptor = ArgumentCaptor.forClass((Class<List<BarberWorkBlock>>) (Class<?>) List.class);
        verify(workBlockRepository).saveAll(blocksCaptor.capture());
        assertThat(blocksCaptor.getValue()).hasSize(3);
        assertThat(barber.getWorkStartTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(barber.getWorkEndTime()).isEqualTo(LocalTime.of(18, 0));
        verify(barberRepository).save(barber);
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldClearLegacyWorkHoursWhenScheduleIsEmpty() {
        UUID barberId = UUID.randomUUID();
        Barber barber = Barber.builder()
                .id(barberId)
                .firebaseUid("firebase-1")
                .workStartTime(LocalTime.of(9, 0))
                .workEndTime(LocalTime.of(18, 0))
                .build();

        when(barberRepository.findByFirebaseUid("firebase-1")).thenReturn(Optional.of(barber));
        when(workBlockRepository.findByBarberIdOrderByDayOfWeekAscStartTimeAsc(barberId)).thenReturn(List.of());

        List<DayScheduleDTO> result = service.saveSchedule("firebase-1", new SaveWeekScheduleDTO(List.of()));

        verify(workBlockRepository).deleteByBarberId(barberId);
        verify(workBlockRepository).saveAll(List.of());
        assertThat(barber.getWorkStartTime()).isNull();
        assertThat(barber.getWorkEndTime()).isNull();
        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectBlocksWithInvalidTimeRange() {
        SaveWeekScheduleDTO request = new SaveWeekScheduleDTO(List.of(
                new DayScheduleDTO(DayOfWeek.MONDAY, List.of(
                        new WorkBlockDTO(LocalTime.of(12, 0), LocalTime.of(12, 0))
                ))
        ));
        when(barberRepository.findByFirebaseUid("firebase-1"))
                .thenReturn(Optional.of(Barber.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> service.saveSchedule("firebase-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Horário de início deve ser anterior");

        verify(workBlockRepository, never()).deleteByBarberId(org.mockito.ArgumentMatchers.any());
        verify(workBlockRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldRejectOverlappingBlocksInSameDay() {
        SaveWeekScheduleDTO request = new SaveWeekScheduleDTO(List.of(
                new DayScheduleDTO(DayOfWeek.MONDAY, List.of(
                        new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new WorkBlockDTO(LocalTime.of(11, 30), LocalTime.of(14, 0))
                ))
        ));
        when(barberRepository.findByFirebaseUid("firebase-1"))
                .thenReturn(Optional.of(Barber.builder().id(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> service.saveSchedule("firebase-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("se sobrepõem");

        verify(workBlockRepository, never()).deleteByBarberId(org.mockito.ArgumentMatchers.any());
        verify(workBlockRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldFailWhenBarberIsNotFound() {
        when(barberRepository.findByFirebaseUid("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSchedule("missing"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Barbeiro não encontrado.");
    }

    private BarberWorkBlock block(UUID barberId, DayOfWeek dayOfWeek, String start, String end) {
        return BarberWorkBlock.builder()
                .barberId(barberId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .build();
    }
}
