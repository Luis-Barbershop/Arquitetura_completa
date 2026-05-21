package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.BarbershopInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.RescheduleAppointmentDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.WorkBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.event.AppointmentCancelledEvent;
import ifsp.edu.projeto.cortaai.scheduleservice.event.AppointmentConcludedEvent;
import ifsp.edu.projeto.cortaai.scheduleservice.event.AppointmentRescheduledEvent;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ConflictException;
import ifsp.edu.projeto.cortaai.scheduleservice.exception.ForbiddenException;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
class AppointmentServiceInteractionsTest {

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

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache appointmentAvailabilityCache;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void shouldAllowCustomerToConcludeOwnAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.SCHEDULED);

        UserInfoDTO caller = new UserInfoDTO();
        caller.setId(customerId);
        caller.setEmail("customer@cortaai.com");

        UserInfoDTO customerInfo = new UserInfoDTO();
        customerInfo.setId(customerId);
        customerInfo.setEmail("customer@cortaai.com");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userServiceClient.getUserByEmail("customer@cortaai.com")).thenReturn(caller);
        when(userServiceClient.getUserById(customerId)).thenReturn(customerInfo);

        appointmentService.concludeAppointment("customer@cortaai.com", appointmentId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepository).save(appointment);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("appointment.concluded"), any(AppointmentConcludedEvent.class));
    }

    @Test
    void shouldAllowOwnerToCancelAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.CONFIRMED);

        UserInfoDTO owner = new UserInfoDTO();
        owner.setId(ownerId);
        owner.setEmail("owner@cortaai.com");

        BarbershopInfoDTO barbershop = new BarbershopInfoDTO();
        barbershop.setId(shopId);
        barbershop.setOwnerId(ownerId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userServiceClient.getUserByEmail("owner@cortaai.com")).thenReturn(owner);
        when(barbershopServiceClient.getBarbershopById(shopId)).thenReturn(barbershop);
        mockAvailabilityCache();

        appointmentService.cancelAppointment("owner@cortaai.com", appointmentId);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("appointment.cancelled"), any(AppointmentCancelledEvent.class));
    }

    @Test
    void shouldRescheduleAndResetStatusWhenAppointmentWasConfirmed() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.CONFIRMED);
        LocalDateTime newStart = LocalDateTime.now()
                .plusDays(7)
                .withHour(11)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        UserInfoDTO barber = new UserInfoDTO();
        barber.setId(barberId);
        barber.setEmail("barber@cortaai.com");

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userServiceClient.getUserByEmail("barber@cortaai.com")).thenReturn(barber);
        when(userServiceClient.getBarberWorkSchedule(barberId))
                .thenReturn(List.of(new DayScheduleDTO(newStart.getDayOfWeek(), List.of(
                        new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(18, 0))
                ))));
        when(appointmentRepository.findConflictsForUpdateExcludingAppointment(eq(barberId), eq(appointmentId), eq(newStart), eq(newStart.plusMinutes(30))))
                .thenReturn(List.of());
        when(barberBlockRepository.existsByBarberIdAndStartTimeLessThanAndEndTimeGreaterThan(eq(barberId), eq(newStart.plusMinutes(30)), eq(newStart)))
                .thenReturn(false);
        mockAvailabilityCache();

        appointmentService.rescheduleAppointment("barber@cortaai.com", appointmentId, new RescheduleAppointmentDTO(newStart, null));

        assertThat(appointment.getStartTime()).isEqualTo(newStart);
        assertThat(appointment.getEndTime()).isEqualTo(newStart.plusMinutes(30));
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        verify(appointmentRepository).save(appointment);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq("appointment.rescheduled"), any(AppointmentRescheduledEvent.class));
    }

    @Test
    void shouldDenyWhenCallerIsNotCustomerBarberOrOwner() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.SCHEDULED);

        UserInfoDTO stranger = new UserInfoDTO();
        stranger.setId(strangerId);
        stranger.setEmail("stranger@cortaai.com");

        BarbershopInfoDTO barbershop = new BarbershopInfoDTO();
        barbershop.setId(shopId);
        barbershop.setOwnerId(ownerId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(userServiceClient.getUserByEmail("stranger@cortaai.com")).thenReturn(stranger);
        when(barbershopServiceClient.getBarbershopById(shopId)).thenReturn(barbershop);

        assertThatThrownBy(() -> appointmentService.concludeAppointment("stranger@cortaai.com", appointmentId))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Você não tem permissão para concluir este agendamento.");

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void shouldDenyConcludingAppointmentWithPendingPayment() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.PAYMENT_PENDING);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.concludeAppointment("barber@cortaai.com", appointmentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Agendamentos com pagamento pendente não podem ser concluídos.");

        verify(userServiceClient, never()).getUserByEmail(any(String.class));
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void shouldUpdateAppointmentToPaymentPendingFromPaymentService() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.SCHEDULED);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        appointmentService.updatePaymentStatus(appointmentId, "\"PAYMENT_PENDING\"");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PAYMENT_PENDING);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void shouldCancelExpiredPaymentPendingAppointments() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.PAYMENT_PENDING);

        when(appointmentRepository.findExpiredPaymentPendingAppointments(any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));
        mockAvailabilityCache();

        int cancelled = appointmentService.cancelExpiredPaymentPendingAppointments();

        assertThat(cancelled).isEqualTo(1);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void shouldCompleteAppointmentsAfterEndTime() {
        UUID appointmentId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        Appointment appointment = buildAppointment(appointmentId, customerId, barberId, shopId, AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findAppointmentsReadyForAutoCompletion(any(LocalDateTime.class)))
                .thenReturn(List.of(appointment));

        int completed = appointmentService.completeAppointmentsAfterEndTime();

        assertThat(completed).isEqualTo(1);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        verify(appointmentRepository).save(appointment);
    }

    private Appointment buildAppointment(UUID appointmentId,
                                         UUID customerId,
                                         UUID barberId,
                                         UUID shopId,
                                         AppointmentStatus status) {
        return Appointment.builder()
                .id(appointmentId)
                .customerId(customerId)
                .barberId(barberId)
                .barbershopId(shopId)
                .customerName("Cliente Teste")
                .barberName("Barbeiro Teste")
                .barbershopName("Barbearia Teste")
                .startTime(LocalDateTime.of(2026, 4, 24, 10, 0))
                .endTime(LocalDateTime.of(2026, 4, 24, 10, 30))
                .totalPrice(BigDecimal.TEN)
                .status(status)
                .build();
    }

    private void mockAvailabilityCache() {
        when(cacheManager.getCache("appointmentAvailability")).thenReturn(appointmentAvailabilityCache);
    }
}
