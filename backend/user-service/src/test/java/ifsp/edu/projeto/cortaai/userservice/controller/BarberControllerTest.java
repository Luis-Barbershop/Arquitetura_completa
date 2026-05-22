package ifsp.edu.projeto.cortaai.userservice.controller;

import ifsp.edu.projeto.cortaai.userservice.dto.AssignActivitiesDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.BarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.SaveWeekScheduleDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.UpdateBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.service.BarberService;
import ifsp.edu.projeto.cortaai.userservice.service.BarberWorkScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarberControllerTest {

    @Mock
    private BarberService barberService;
    @Mock
    private BarberWorkScheduleService workScheduleService;
    @Mock
    private MultipartFile file;

    private BarberController controller;

    @BeforeEach
    void setUp() {
        controller = new BarberController(barberService, workScheduleService);
    }

    @Test
    void shouldReadAndUpdateBarbers() {
        UUID barberId = UUID.randomUUID();
        UUID barbershopId = UUID.randomUUID();
        UpdateBarberDTO updateDTO = new UpdateBarberDTO();
        updateDTO.setName("Atualizado");
        BarberDTO barber = barber(barberId);
        when(barberService.update(barberId, updateDTO)).thenReturn(barber);
        when(barberService.findById(barberId)).thenReturn(barber);
        when(barberService.findAll()).thenReturn(List.of(barber));
        when(barberService.findByBarbershopId(barbershopId)).thenReturn(List.of(barber));

        assertThat(controller.updateBarber(barberId, updateDTO).getBody()).isEqualTo(barber);
        assertThat(controller.getBarberById(barberId).getBody()).isEqualTo(barber);
        assertThat(controller.getAllBarbers().getBody()).containsExactly(barber);
        assertThat(controller.getBarbersByBarbershop(barbershopId).getBody()).containsExactly(barber);
    }

    @Test
    void shouldManageAssignedActivities() {
        UUID barberId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        AssignActivitiesDTO dto = new AssignActivitiesDTO(List.of(activityId));
        when(barberService.getAssignedActivityIdsById(barberId)).thenReturn(Set.of(activityId));
        when(barberService.getAssignedActivityIds("firebase-uid")).thenReturn(Set.of(activityId));
        when(barberService.assignActivities("firebase-uid", dto)).thenReturn(Set.of(activityId));

        assertThat(controller.getBarberActivities(barberId).getBody()).containsExactly(activityId);
        assertThat(controller.getMyActivities("firebase-uid").getBody()).containsExactly(activityId);
        assertThat(controller.assignActivities("firebase-uid", dto).getBody()).containsExactly(activityId);
    }

    @Test
    void shouldReadAndSaveWorkSchedules() {
        UUID barberId = UUID.randomUUID();
        DayScheduleDTO day = new DayScheduleDTO(DayOfWeek.MONDAY, List.of());
        SaveWeekScheduleDTO dto = new SaveWeekScheduleDTO(List.of(day));
        when(workScheduleService.getSchedule("firebase-uid")).thenReturn(List.of(day));
        when(workScheduleService.saveSchedule("firebase-uid", dto)).thenReturn(List.of(day));
        when(workScheduleService.getScheduleByBarberId(barberId)).thenReturn(List.of(day));

        assertThat(controller.getMyWorkSchedule("firebase-uid").getBody()).containsExactly(day);
        assertThat(controller.saveMyWorkSchedule("firebase-uid", dto).getBody()).containsExactly(day);
        assertThat(controller.getBarberWorkSchedule(barberId).getBody()).containsExactly(day);
    }

    @Test
    void shouldUploadBarberPhoto() {
        when(barberService.updateProfilePhotoByFirebaseUid("firebase-uid", file)).thenReturn("https://cdn/barber.png");

        var response = controller.uploadBarberPhoto("firebase-uid", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("https://cdn/barber.png");
        verify(barberService).updateProfilePhotoByFirebaseUid("firebase-uid", file);
    }

    private BarberDTO barber(UUID id) {
        return new BarberDTO(
                id,
                "Barbeiro",
                "barber@example.com",
                "11999999999",
                "12345678909",
                LocalDate.of(1990, 1, 1),
                false,
                true,
                UUID.randomUUID(),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "https://cdn/barber.png",
                Set.of(UUID.randomUUID())
        );
    }
}
