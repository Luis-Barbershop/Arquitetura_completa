package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import ifsp.edu.projeto.cortaai.userservice.model.Barber;
import ifsp.edu.projeto.cortaai.userservice.model.Customer;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
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

    private final CustomerRepository customerRepository;
    private final BarberRepository barberRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            List<Customer> customers = customerRepository.findWithLegacyPlainSensitiveData();
            if (!customers.isEmpty()) {
                customerRepository.saveAll(customers);
            }

            List<Barber> barbers = barberRepository.findWithLegacyPlainSensitiveData();
            if (!barbers.isEmpty()) {
                barberRepository.saveAll(barbers);
            }

            int migrated = customers.size() + barbers.size();
            if (migrated > 0) {
                log.info("event=sensitive-data-backfill status=completed migratedRecords={}", migrated);
            }
        } catch (Exception e) {
            log.warn("event=sensitive-data-backfill status=skipped reason=\"{}\" — " +
                    "provável dado binário legado em barber_assigned_activities; truncate a tabela para resolver.",
                    e.getMessage());
        }
    }
}
