package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.util.List;
import java.util.UUID;

public record FinancialSeriesDTO(
        UUID barbershopId,
        String groupBy,
        List<FinancialSeriesPointDTO> points
) {}

