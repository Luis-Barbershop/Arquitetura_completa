package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.model.analytics.VBarberFinancialPerformance;
import ifsp.edu.projeto.cortaai.paymentservice.repository.analytics.VBarberFinancialPerformanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAnalyticsServiceTest {

    @Mock
    private VBarberFinancialPerformanceRepository repository;

    @InjectMocks
    private PaymentAnalyticsService service;

    @Test
    void shouldMapBarberFinancialPerformanceViewRows() {
        UUID shopId = UUID.randomUUID();
        VBarberFinancialPerformance row = new VBarberFinancialPerformance();
        ReflectionTestUtils.setField(row, "barberId", "barber-1");
        ReflectionTestUtils.setField(row, "barberName", "Ana");
        ReflectionTestUtils.setField(row, "barbershopId", shopId);
        ReflectionTestUtils.setField(row, "totalAppointments", 3L);
        ReflectionTestUtils.setField(row, "generatedRevenue", new BigDecimal("150.00"));
        ReflectionTestUtils.setField(row, "contributionPercentage", new BigDecimal("37.50"));

        when(repository.findByBarbershopId(shopId)).thenReturn(List.of(row));

        List<BarberFinancialPerformanceResponseDTO> result = service.getBarberFinancialPerformance(shopId);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.barberId()).isEqualTo("barber-1");
            assertThat(dto.barberName()).isEqualTo("Ana");
            assertThat(dto.totalAppointments()).isEqualTo(3L);
            assertThat(dto.generatedRevenue()).isEqualByComparingTo("150.00");
            assertThat(dto.contributionPercentage()).isEqualByComparingTo("37.50");
            assertThat(dto.barbershopCommission()).isEqualByComparingTo("150.00");
        });
    }
}
