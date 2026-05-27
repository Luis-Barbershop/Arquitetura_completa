package ifsp.edu.projeto.cortaai.paymentservice.controller;

import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialPerformanceResponseDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.BarberFinancialSummaryDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.CreatePaymentDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialOverviewDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.FinancialSeriesPointDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.MpConnectionStatusDTO;
import ifsp.edu.projeto.cortaai.paymentservice.dto.TransactionDTO;
import ifsp.edu.projeto.cortaai.paymentservice.model.PaymentStatus;
import ifsp.edu.projeto.cortaai.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @InjectMocks
    private PaymentAnalyticsController paymentAnalyticsController;

    @Test
    void shouldDelegatePaymentEndpoints() {
        UUID appointmentId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        String firebaseUid = "firebase-uid";
        CreatePaymentDTO request = new CreatePaymentDTO(appointmentId, "PIX");
        TransactionDTO transaction = transactionDto(transactionId, appointmentId);

        when(paymentService.createPaymentByFirebaseUid(appointmentId, firebaseUid, "PIX")).thenReturn(transaction);
        when(paymentService.getById(transactionId)).thenReturn(transaction);
        when(paymentService.getMyPaymentsByFirebaseUid(firebaseUid)).thenReturn(List.of(transaction));

        assertThat(paymentController.createPayment(request, firebaseUid).getBody()).isEqualTo(transaction);
        assertThat(paymentController.getById(transactionId).getBody()).isEqualTo(transaction);
        assertThat(paymentController.getMyPayments(firebaseUid).getBody()).containsExactly(transaction);
    }

    @Test
    void shouldDelegateMercadoPagoConnectionEndpoints() {
        String firebaseUid = "owner-uid";
        MpConnectionStatusDTO status = new MpConnectionStatusDTO(true, "1234...99", true);

        when(paymentService.getMpConnectionStatusByFirebaseUid(firebaseUid)).thenReturn(status);

        assertThat(paymentController.getMpConnectionStatus(firebaseUid).getBody()).isEqualTo(status);

        ResponseEntity<Void> disconnectResponse = paymentController.disconnectMpConnection(firebaseUid);
        assertThat(disconnectResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(paymentService).disconnectMpByFirebaseUid(firebaseUid);
    }

    @Test
    void shouldDelegateFinancialDashboardEndpoints() {
        String firebaseUid = "owner-uid";
        UUID shopId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 22);
        FinancialOverviewDTO overview = new FinancialOverviewDTO(shopId, "BRL",
                new BigDecimal("100.00"), new BigDecimal("25.00"), new BigDecimal("125.00"),
                new BigDecimal("10.00"), new BigDecimal("300.00"), new BigDecimal("90.00"),
                new BigDecimal("115.00"), 4, 1, 3, 1, 0);
        BarberFinancialSummaryDTO summary = new BarberFinancialSummaryDTO(
                shopId, UUID.randomUUID(), "Ana", "BRL",
                new BigDecimal("80.00"), new BigDecimal("20.00"), new BigDecimal("100.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00"), new BigDecimal("50.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00"), new BigDecimal("50.00"),
                2, 1, 2, 0, 0);
        FinancialSeriesDTO series = new FinancialSeriesDTO(shopId, "DAY",
                List.of(new FinancialSeriesPointDTO(from, new BigDecimal("100.00"),
                        new BigDecimal("20.00"), new BigDecimal("120.00"), 2, 1)));
        BarberFinancialPerformanceResponseDTO performance = new BarberFinancialPerformanceResponseDTO(
                "barber-1", "Ana", 2L, new BigDecimal("80.00"), new BigDecimal("100.00"));

        when(paymentService.getBarbershopOverviewByFirebaseUid(firebaseUid, shopId, from, to)).thenReturn(overview);
        when(paymentService.getBarberFinancialSummaryByFirebaseUid(firebaseUid, shopId, from, to)).thenReturn(summary);
        when(paymentService.getBarbershopSeriesByFirebaseUid(firebaseUid, shopId, from, to, "WEEK")).thenReturn(series);
        when(paymentService.getBarberFinancialPerformance(firebaseUid, shopId, from, to)).thenReturn(List.of(performance));

        assertThat(paymentController.getMyShopOverview(firebaseUid, shopId, from, to).getBody()).isEqualTo(overview);
        assertThat(paymentController.getMyBarberSummary(firebaseUid, shopId, from, to).getBody()).isEqualTo(summary);
        assertThat(paymentController.getMyShopSeries(firebaseUid, shopId, from, to, "WEEK").getBody()).isEqualTo(series);
        assertThat(paymentController.getBarberPerformance(firebaseUid, shopId, from, to).getBody()).containsExactly(performance);
    }

    @Test
    void shouldDelegateAnalyticsController() {
        String firebaseUid = "owner-uid";
        UUID shopId = UUID.randomUUID();
        BarberFinancialPerformanceResponseDTO performance = new BarberFinancialPerformanceResponseDTO(
                "barber-1", "Ana", 2L, new BigDecimal("80.00"), new BigDecimal("100.00"));

        when(paymentService.getBarberFinancialPerformance(firebaseUid, shopId)).thenReturn(List.of(performance));

        assertThat(paymentAnalyticsController.getBarberPerformance(firebaseUid, shopId).getBody()).containsExactly(performance);
    }

    private TransactionDTO transactionDto(UUID transactionId, UUID appointmentId) {
        return new TransactionDTO(transactionId, appointmentId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("48.00"),
                new BigDecimal("2.00"), BigDecimal.ZERO, "PIX", PaymentStatus.PENDING,
                "https://checkout.test", null, null);
    }
}
