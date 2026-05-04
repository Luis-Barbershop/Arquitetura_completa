package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AppointmentDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ForbiddenException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.NotFoundException;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.mapper.AppointmentMapper;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.BarberBlockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceMasterScheduleAuthorizationTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BarberBlockRepository barberBlockRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private BarbershopServiceClient barbershopServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void shouldAllowOwnerAndReturnBarbershopSchedule() {
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 16);

        UserInfoDTO caller = new UserInfoDTO();
        caller.setId(ownerId);
        caller.setUserType("BARBER");
        caller.setBarbershopId(shopId);

        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .barberId(UUID.randomUUID())
                .barbershopId(shopId)
                .customerName("Cliente Teste")
                .barberName("Barbeiro Teste")
                .barbershopName("Barbearia Teste")
                .startTime(date.atTime(9, 0))
                .endTime(date.atTime(9, 30))
                .totalPrice(BigDecimal.TEN)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());

        when(userServiceClient.getUserByEmail("owner@cortaai.com")).thenReturn(caller);
        when(appointmentRepository.findByBarbershopIdAndStartTimeBetween(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));
        when(appointmentMapper.toDTO(appointment)).thenReturn(dto);

        List<AppointmentDTO> result = appointmentService.getBarbershopSchedule(shopId, date, "owner@cortaai.com", "cid-123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(appointment.getId());
        verify(appointmentRepository).findByBarbershopIdAndStartTimeBetween(eq(shopId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldDenyAccessForNonOwner() {
        UUID shopId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 16);

        UserInfoDTO caller = new UserInfoDTO();
        caller.setId(callerId);
        caller.setUserType("BARBER");
        caller.setBarbershopId(UUID.randomUUID());

        when(userServiceClient.getUserByEmail("barber@cortaai.com")).thenReturn(caller);

        assertThatThrownBy(() -> appointmentService.getBarbershopSchedule(shopId, date, "barber@cortaai.com", "cid-456"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Apenas barbeiros vinculados a esta barbearia podem visualizar a agenda da equipe.");

        verify(appointmentRepository, never()).findByBarbershopIdAndStartTimeBetween(any(), any(), any());
        verify(appointmentMapper, never()).toDTO(any());
    }

    @Test
    void shouldFailWhenAuthenticatedUserWasNotFound() {
        UUID shopId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 16);

        when(userServiceClient.getUserByEmail("ghost@cortaai.com")).thenReturn(null);

        assertThatThrownBy(() -> appointmentService.getBarbershopSchedule(shopId, date, "ghost@cortaai.com", "cid-789"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Usuário autenticado não encontrado.");

        verify(barbershopServiceClient, never()).getBarbershopById(any());
        verify(appointmentRepository, never()).findByBarbershopIdAndStartTimeBetween(any(), any(), any());
    }
}
