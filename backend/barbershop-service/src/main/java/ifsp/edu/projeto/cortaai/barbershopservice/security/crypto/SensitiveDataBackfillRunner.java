package ifsp.edu.projeto.cortaai.barbershopservice.security.crypto;

import ifsp.edu.projeto.cortaai.barbershopservice.model.Barbershop;
import ifsp.edu.projeto.cortaai.barbershopservice.repository.BarbershopRepository;
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

    private final BarbershopRepository barbershopRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Barbershop> barbershops = barbershopRepository.findWithLegacyPlainCnpj();
        if (!barbershops.isEmpty()) {
            barbershopRepository.saveAll(barbershops);
            log.info("event=sensitive-data-backfill status=completed migratedRecords={}", barbershops.size());
        }
    }
}
