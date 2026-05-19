package ifsp.edu.projeto.cortaai.scheduleservice.messaging;

import ifsp.edu.projeto.cortaai.scheduleservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentLifecycleScheduler {

    private final AppointmentService appointmentService;

    @Scheduled(fixedDelayString = "${app.appointments.lifecycle-scheduler-delay-ms:300000}")
    public void processLifecycleTransitions() {
        int cancelled = appointmentService.cancelExpiredPaymentPendingAppointments();
        int completed = appointmentService.completeAppointmentsAfterEndTime();

        if (cancelled > 0 || completed > 0) {
            log.info(
                    "AppointmentLifecycleScheduler: {} agendamentos cancelados por pagamento pendente; {} concluidos automaticamente.",
                    cancelled,
                    completed
            );
        }
    }
}
