package ifsp.edu.projeto.cortaai.scheduleservice.messaging;

import ifsp.edu.projeto.cortaai.scheduleservice.config.RabbitConfig;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.event.AppointmentReminderEvent;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Publica evento de lembrete quando falta 1h ou menos para o agendamento.
 * Roda a cada 5 minutos e usa Redis para evitar duplicidade.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceClient userServiceClient;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final Set<String> localReminderFallback = ConcurrentHashMap.newKeySet();

    @Value("${app.timezone:UTC}")
    private String appTimezone;

    @Scheduled(fixedDelay = 300_000) // 5 minutos
    public void publishReminders() {
        LocalDateTime now = getNowInAppTimezone();
        LocalDateTime to = now.plusHours(1);

        List<Appointment> appointments = appointmentRepository.findAppointmentsForReminderWindow(now, to);
        log.info("ReminderScheduler: {} agendamentos na janela [{}, {}]", appointments.size(), now, to);

        for (Appointment appt : appointments) {
            if (!claimReminder(appt, now)) {
                continue;
            }

            String customerEmail = resolveEmail(appt.getCustomerId());
            String barberEmail = resolveEmail(appt.getBarberId());
            AppointmentReminderEvent event = new AppointmentReminderEvent(
                    appt.getId(),
                    appt.getCustomerId(),
                    appt.getCustomerName(),
                    customerEmail,
                    appt.getBarberId(),
                    barberEmail,
                    appt.getBarbershopName(),
                    appt.getBarberName(),
                    appt.getStartTime()
            );
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_APPOINTMENT_REMINDER, event);
            log.info("Lembrete publicado: appointmentId={} customerId={}", appt.getId(), appt.getCustomerId());
        }
    }

    private boolean claimReminder(Appointment appointment, LocalDateTime now) {
        String key = reminderKey(appointment);
        Duration ttl = Duration.between(now, appointment.getStartTime().plusHours(2));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofHours(1);
        }

        try {
            Boolean claimed = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
            return Boolean.TRUE.equals(claimed);
        } catch (Exception ex) {
            log.warn("Redis indisponivel para deduplicar lembrete; usando fallback local. appointmentId={} cause={}",
                    appointment.getId(), ex.getClass().getSimpleName());
            return localReminderFallback.add(key);
        }
    }

    private String reminderKey(Appointment appointment) {
        return "schedule:appointment-reminder:%s:%s".formatted(
                appointment.getId(),
                appointment.getStartTime());
    }

    private String resolveEmail(java.util.UUID userId) {
        try {
            UserInfoDTO user = userServiceClient.getUserById(userId);
            return user != null ? user.getEmail() : null;
        } catch (Exception e) {
            log.warn("Não foi possível resolver email para userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    private LocalDateTime getNowInAppTimezone() {
        try {
            String timezone = (appTimezone == null || appTimezone.isBlank())
                    ? "UTC"
                    : appTimezone.trim();
            return LocalDateTime.now(ZoneId.of(timezone));
        } catch (DateTimeException ex) {
            log.warn("Timezone inválido em app.timezone='{}'; usando UTC.", appTimezone);
            return LocalDateTime.now(ZoneId.of("UTC"));
        }
    }
}
