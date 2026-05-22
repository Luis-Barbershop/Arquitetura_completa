package ifsp.edu.projeto.cortaai.userservice.messaging;

import ifsp.edu.projeto.cortaai.userservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.userservice.messaging.event.CustomerDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerDeletedPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldPublishCustomerDeletedEventToConfiguredExchange() {
        UUID customerId = UUID.randomUUID();
        CustomerDeletedPublisher publisher = new CustomerDeletedPublisher(rabbitTemplate);
        ArgumentCaptor<CustomerDeletedEvent> eventCaptor = ArgumentCaptor.forClass(CustomerDeletedEvent.class);

        publisher.publish(customerId);

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitConfig.EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitConfig.RK_CUSTOMER_DELETED),
                eventCaptor.capture()
        );
        assertThat(eventCaptor.getValue().customerId()).isEqualTo(customerId);
    }
}
