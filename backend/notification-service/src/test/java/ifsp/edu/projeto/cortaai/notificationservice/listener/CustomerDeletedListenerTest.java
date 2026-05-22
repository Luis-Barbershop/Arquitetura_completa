package ifsp.edu.projeto.cortaai.notificationservice.listener;

import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import ifsp.edu.projeto.cortaai.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerDeletedListenerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private CustomerDeletedListener listener;

    @BeforeEach
    void setUp() {
        listener = new CustomerDeletedListener(notificationRepository, deviceTokenRepository);
    }

    @Test
    void shouldRemoveNotificationsAndDeviceTokensWhenCustomerIsDeleted() {
        UUID customerId = UUID.randomUUID();

        listener.onCustomerDeleted(Map.of("customerId", customerId.toString()));

        verify(notificationRepository).deleteByUserId(customerId);
        verify(deviceTokenRepository).deleteByUserId(customerId);
    }

    @Test
    void shouldRethrowInvalidPayloadFailures() {
        assertThatThrownBy(() -> listener.onCustomerDeleted(Map.of("customerId", "invalid-uuid")))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(notificationRepository, deviceTokenRepository);
    }
}
