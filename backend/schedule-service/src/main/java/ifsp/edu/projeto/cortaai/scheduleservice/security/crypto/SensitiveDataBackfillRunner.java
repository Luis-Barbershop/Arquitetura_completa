package ifsp.edu.projeto.cortaai.scheduleservice.security.crypto;

import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SensitiveDataBackfillRunner implements ApplicationRunner {

    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Appointment> appointments = appointmentRepository.findWithLegacyPlainCustomerName();
        if (!appointments.isEmpty()) {
            appointmentRepository.saveAll(appointments);
            log.info("event=sensitive-data-backfill status=completed migratedRecords={}", appointments.size());
        }
    }
}
