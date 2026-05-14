package ifsp.edu.projeto.cortaai.paymentservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO inter-serviço — informações do agendamento vindas do schedule-service.
 */
public record AppointmentInfoDTO(
        UUID id,
        UUID customerId,
        UUID barberId,
        UUID barbershopId,
        String customerName,
        String barberName,
        String barbershopName,
        LocalDateTime startTime,
        BigDecimal totalPrice,
        String status,
        List<AppointmentActivityInfoDTO> activities
) {}
