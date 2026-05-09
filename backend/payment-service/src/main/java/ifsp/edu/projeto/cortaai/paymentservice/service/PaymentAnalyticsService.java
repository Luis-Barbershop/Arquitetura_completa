package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.repository.analytics.VBarberFinancialPerformanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentAnalyticsService {

    private final VBarberFinancialPerformanceRepository vBarberFinancialPerformanceRepository;

    public List<BarberFinancialPerformanceResponseDTO> getBarberFinancialPerformance() {
        log.info("Consultando performance financeira dos barbeiros via view");
        return vBarberFinancialPerformanceRepository.findAll()
                .stream()
                .map(v -> new BarberFinancialPerformanceResponseDTO(
                        v.getBarberId(),
                        v.getBarberName(),
                        v.getTotalAppointments(),
                        v.getGeneratedRevenue(),
                        v.getContributionPercentage()
                ))
                .toList();
    }
}
