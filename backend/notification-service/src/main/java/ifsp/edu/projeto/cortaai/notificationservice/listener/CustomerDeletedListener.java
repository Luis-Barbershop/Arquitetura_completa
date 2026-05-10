package ifsp.edu.projeto.cortaai.notificationservice.listener;

import ifsp.edu.projeto.cortaai.notificationservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.notificationservice.repository.DeviceTokenRepository;
import ifsp.edu.projeto.cortaai.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDeletedListener {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE_CUSTOMER_DELETED)
    @Transactional
    public void onCustomerDeleted(Map<String, Object> payload) {
        try {
            UUID customerId = UUID.fromString(payload.get("customerId").toString());
            notificationRepository.deleteByUserId(customerId);
            deviceTokenRepository.deleteByUserId(customerId);
            log.info("Histórico de notificações e device tokens removidos para customerId={}", customerId);
        } catch (Exception e) {
            log.error("Erro ao remover dados de notificação após exclusão de customer: {}", e.getMessage(), e);
            throw e;
        }
    }
}
