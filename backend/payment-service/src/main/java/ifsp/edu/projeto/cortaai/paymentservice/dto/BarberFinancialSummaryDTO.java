package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resumo financeiro do barbeiro autenticado, separando valor bruto,
 * comissao recebida e parte que fica com a barbearia.
 */
public record BarberFinancialSummaryDTO(
        UUID barbershopId,
        UUID barberId,
        String barberName,
        String currency,
        BigDecimal grossServiceRevenue,
        BigDecimal grossWalkInRevenue,
        BigDecimal grossTotalRevenue,
        BigDecimal barberServiceCommission,
        BigDecimal barberWalkInCommission,
        BigDecimal barberTotalCommission,
        BigDecimal barbershopServiceCommission,
        BigDecimal barbershopWalkInCommission,
        BigDecimal barbershopTotalCommission,
        int transactionsCount,
        int walkInAppointmentsCount,
        int approvedCount,
        int pendingCount,
        int cancelledCount
) {}
