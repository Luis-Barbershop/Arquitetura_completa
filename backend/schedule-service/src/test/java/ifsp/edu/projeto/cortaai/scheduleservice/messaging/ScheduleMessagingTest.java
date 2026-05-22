package ifsp.edu.projeto.cortaai.scheduleservice.messaging;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.event.AppointmentReminderEvent;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleMessagingTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private AppointmentService appointmentService;

    private ReminderScheduler reminderScheduler;

    @BeforeEach
    void setUp() {
        reminderScheduler = new ReminderScheduler(appointmentRepository, rabbitTemplate, userServiceClient, redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldPublishReminderForClaimedAppointments() {
        UUID customerId = UUID.randomUUID();
        Appointment appointment = appointment(customerId, LocalDateTime.now().plusMinutes(45));
        UserInfoDTO user = new UserInfoDTO();
        user.setEmail("cliente@example.com");
        when(appointmentRepository.findAppointmentsForReminderWindow(any(), any())).thenReturn(List.of(appointment));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(userServiceClient.getUserById(customerId)).thenReturn(user);

        reminderScheduler.publishReminders();

        ArgumentCaptor<AppointmentReminderEvent> eventCaptor = ArgumentCaptor.forClass(AppointmentReminderEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.RK_APPOINTMENT_REMINDER), eventCaptor.capture());
        assertThat(eventCaptor.getValue().appointmentId()).isEqualTo(appointment.getId());
        assertThat(eventCaptor.getValue().customerEmail()).isEqualTo("cliente@example.com");
    }

    @Test
    void shouldSkipDuplicateReminderClaimsAndFallbackLocallyWhenRedisFails() {
        Appointment duplicate = appointment(UUID.randomUUID(), LocalDateTime.now().plusMinutes(30));
        Appointment fallback = appointment(UUID.randomUUID(), LocalDateTime.now().minusHours(3));
        when(appointmentRepository.findAppointmentsForReminderWindow(any(), any()))
                .thenReturn(List.of(duplicate))
                .thenReturn(List.of(fallback))
                .thenReturn(List.of(fallback));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(false)
                .thenThrow(new RuntimeException("redis off"))
                .thenThrow(new RuntimeException("redis off"));

        reminderScheduler.publishReminders();
        reminderScheduler.publishReminders();
        reminderScheduler.publishReminders();

        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.RK_APPOINTMENT_REMINDER), any(AppointmentReminderEvent.class));
    }

    @Test
    void shouldReturnNullEmailWhenUserServiceFails() {
        Appointment appointment = appointment(UUID.randomUUID(), LocalDateTime.now().plusMinutes(20));
        when(appointmentRepository.findAppointmentsForReminderWindow(any(), any())).thenReturn(List.of(appointment));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(userServiceClient.getUserById(appointment.getCustomerId())).thenThrow(new RuntimeException("user off"));

        reminderScheduler.publishReminders();

        ArgumentCaptor<AppointmentReminderEvent> eventCaptor = ArgumentCaptor.forClass(AppointmentReminderEvent.class);
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), eventCaptor.capture());
        assertThat(eventCaptor.getValue().customerEmail()).isNull();
    }

    @Test
    void shouldAnonymizeCustomerAppointmentsWhenCustomerIsDeleted() {
        CustomerDeletedListener listener = new CustomerDeletedListener(appointmentRepository);
        UUID customerId = UUID.randomUUID();
        Appointment appointment = appointment(customerId, LocalDateTime.now());
        when(appointmentRepository.findByCustomerIdOrderByStartTimeDesc(customerId)).thenReturn(List.of(appointment));

        listener.onCustomerDeleted(Map.of("customerId", customerId.toString()));

        assertThat(appointment.getCustomerName()).isEqualTo("Cliente Removido");
        verify(appointmentRepository).saveAll(List.of(appointment));
    }

    @Test
    void shouldRethrowInvalidCustomerDeletedPayload() {
        CustomerDeletedListener listener = new CustomerDeletedListener(appointmentRepository);

        assertThatThrownBy(() -> listener.onCustomerDeleted(Map.of("customerId", "invalid")))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void shouldProcessLifecycleTransitions() {
        AppointmentLifecycleScheduler scheduler = new AppointmentLifecycleScheduler(appointmentService);
        when(appointmentService.cancelExpiredPaymentPendingAppointments()).thenReturn(2);
        when(appointmentService.completeAppointmentsAfterEndTime()).thenReturn(1);

        scheduler.processLifecycleTransitions();

        verify(appointmentService).cancelExpiredPaymentPendingAppointments();
        verify(appointmentService).completeAppointmentsAfterEndTime();
    }

    private static Appointment appointment(UUID customerId, LocalDateTime startTime) {
        return Appointment.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .customerName("Cliente")
                .barberName("Barbeiro")
                .barbershopName("Barbearia")
                .startTime(startTime)
                .build();
    }
}
