package ifsp.edu.projeto.cortaai.scheduleservice.messaging;

import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerDeletedListenerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private CustomerDeletedListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        listener = new CustomerDeletedListener(appointmentRepository, stringRedisTemplate);
    }

    @Test
    void shouldAnonymizeAppointmentsOnFirstDelivery() {
        UUID customerId = UUID.randomUUID();
        Appointment a1 = new Appointment(); a1.setCustomerName("João Silva");
        Appointment a2 = new Appointment(); a2.setCustomerName("João Silva");

        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(appointmentRepository.findByCustomerIdOrderByStartTimeDesc(customerId)).thenReturn(List.of(a1, a2));

        listener.onCustomerDeleted(Map.of("customerId", customerId.toString()));

        assertThat(a1.getCustomerName()).isEqualTo("Cliente Removido");
        assertThat(a2.getCustomerName()).isEqualTo("Cliente Removido");
        verify(appointmentRepository).saveAll(List.of(a1, a2));
    }

    @Test
    void shouldSkipDuplicateDelivery() {
        UUID customerId = UUID.randomUUID();
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        listener.onCustomerDeleted(Map.of("customerId", customerId.toString()));

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void shouldProcessWhenRedisUnavailable() {
        UUID customerId = UUID.randomUUID();
        Appointment a = new Appointment(); a.setCustomerName("Maria");

        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis offline"));
        when(appointmentRepository.findByCustomerIdOrderByStartTimeDesc(customerId)).thenReturn(List.of(a));

        listener.onCustomerDeleted(Map.of("customerId", customerId.toString()));

        assertThat(a.getCustomerName()).isEqualTo("Cliente Removido");
        verify(appointmentRepository).saveAll(List.of(a));
    }

    @Test
    void shouldRethrowOnInvalidPayload() {
        assertThatThrownBy(() -> listener.onCustomerDeleted(Map.of("customerId", "not-a-uuid")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
