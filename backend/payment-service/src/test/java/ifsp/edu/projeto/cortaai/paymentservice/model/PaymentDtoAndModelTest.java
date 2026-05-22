package ifsp.edu.projeto.cortaai.paymentservice.model;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.CreatePaymentDTO;
import ifsp.edu.projeto.cortaai.paymentservice.model.analytics.VBarberFinancialPerformance;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDtoAndModelTest {

    @Test
    void shouldCreatePaymentRequestDto() {
        UUID appointmentId = UUID.randomUUID();
        CreatePaymentDTO dto = new CreatePaymentDTO(appointmentId, "LOCAL");

        assertThat(dto.appointmentId()).isEqualTo(appointmentId);
        assertThat(dto.paymentMethod()).isEqualTo("LOCAL");
    }

    @Test
    void shouldApplyDefaultFinancialPerformanceValues() {
        BarberFinancialPerformanceResponseDTO dto = new BarberFinancialPerformanceResponseDTO(
                "barber-1", "Ana", 4L, new BigDecimal("200.00"), new BigDecimal("50.00"));

        assertThat(dto.barberCommission()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.barbershopCommission()).isEqualByComparingTo("200.00");
        assertThat(dto.averageTicket()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldUpdateDashboardKpiTimestamp() {
        DashboardKpiDaily kpi = DashboardKpiDaily.builder()
                .barbershopId(UUID.randomUUID())
                .referenceDate(LocalDate.of(2026, 5, 22))
                .build();

        assertThat(kpi.getApprovedRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(kpi.getApprovedTransactionsCount()).isZero();

        kpi.touchUpdatedAt();

        assertThat(kpi.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldExposeBarberFinancialPerformanceViewColumns() {
        UUID shopId = UUID.randomUUID();
        VBarberFinancialPerformance view = new VBarberFinancialPerformance();
        ReflectionTestUtils.setField(view, "barberId", "barber-1");
        ReflectionTestUtils.setField(view, "barberName", "Ana");
        ReflectionTestUtils.setField(view, "barbershopId", shopId);
        ReflectionTestUtils.setField(view, "totalAppointments", 7L);
        ReflectionTestUtils.setField(view, "generatedRevenue", new BigDecimal("350.00"));
        ReflectionTestUtils.setField(view, "contributionPercentage", new BigDecimal("64.20"));

        assertThat(view.getBarberId()).isEqualTo("barber-1");
        assertThat(view.getBarberName()).isEqualTo("Ana");
        assertThat(view.getBarbershopId()).isEqualTo(shopId);
        assertThat(view.getTotalAppointments()).isEqualTo(7L);
        assertThat(view.getGeneratedRevenue()).isEqualByComparingTo("350.00");
        assertThat(view.getContributionPercentage()).isEqualByComparingTo("64.20");
    }
}
