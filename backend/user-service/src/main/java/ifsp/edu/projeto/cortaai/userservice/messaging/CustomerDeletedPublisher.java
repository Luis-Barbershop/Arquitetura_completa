package ifsp.edu.projeto.cortaai.userservice.messaging;

import ifsp.edu.projeto.cortaai.userservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.userservice.messaging.event.CustomerDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDeletedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(UUID customerId) {
        CustomerDeletedEvent event = new CustomerDeletedEvent(customerId);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_CUSTOMER_DELETED, event);
        log.info("Evento customer.deleted publicado para customerId={}", customerId);
    }
}
