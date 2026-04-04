package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSeriesPointDTO(
        LocalDate date,
        BigDecimal serviceRevenue,
        int approvedTransactions
) {}

