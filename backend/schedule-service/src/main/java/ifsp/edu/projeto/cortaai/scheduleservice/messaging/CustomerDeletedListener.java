package ifsp.edu.projeto.cortaai.scheduleservice.messaging;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerDeletedListener {

    private final AppointmentRepository appointmentRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private static final Duration DEDUP_TTL = Duration.ofHours(48);

    @RabbitListener(queues = RabbitConfig.QUEUE_CUSTOMER_DELETED)
    @Transactional
    public void onCustomerDeleted(Map<String, Object> payload) {
        try {
            UUID customerId = UUID.fromString(payload.get("customerId").toString());

            if (isDuplicate(customerId)) {
                log.warn("event=customer-deleted-duplicate-skipped customerId={}", customerId);
                return;
            }

            List<Appointment> appointments = appointmentRepository.findByCustomerIdOrderByStartTimeDesc(customerId);
            for (Appointment a : appointments) {
                a.setCustomerName("Cliente Removido");
            }
            appointmentRepository.saveAll(appointments);
            log.info("Anonimizados {} agendamentos do customerId={}", appointments.size(), customerId);
        } catch (Exception e) {
            log.error("Erro ao anonimizar agendamentos após exclusão de customer: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isDuplicate(UUID customerId) {
        String dedupKey = "schedule:customer-deleted:" + customerId;
        try {
            Boolean claimed = stringRedisTemplate.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_TTL);
            return Boolean.FALSE.equals(claimed);
        } catch (Exception redisEx) {
            log.warn("Redis indisponivel para deduplicar customer.deleted; processando mesmo assim. customerId={} cause={}", customerId, redisEx.getMessage());
            return false;
        }
    }
}
