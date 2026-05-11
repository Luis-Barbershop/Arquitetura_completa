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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Publica evento de lembrete 1h antes do agendamento.
 * Roda a cada 5 minutos e busca agendamentos na janela [+55min, +65min].
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UserServiceClient userServiceClient;

    @Scheduled(fixedDelay = 300_000) // 5 minutos
    public void publishReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusMinutes(55);
        LocalDateTime to = now.plusMinutes(65);

        List<Appointment> appointments = appointmentRepository.findActiveInTimeWindow(from, to);
        log.info("ReminderScheduler: {} agendamentos na janela [{}, {}]", appointments.size(), from, to);

        for (Appointment appt : appointments) {
            String customerEmail = resolveEmail(appt.getCustomerId());
            AppointmentReminderEvent event = new AppointmentReminderEvent(
                    appt.getId(),
                    appt.getCustomerId(),
                    appt.getCustomerName(),
                    customerEmail,
                    appt.getBarbershopName(),
                    appt.getBarberName(),
                    appt.getStartTime()
            );
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RK_APPOINTMENT_REMINDER, event);
            log.info("Lembrete publicado: appointmentId={} customerId={}", appt.getId(), appt.getCustomerId());
        }
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
}
