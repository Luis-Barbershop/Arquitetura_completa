package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Dados de comissão de um barbeiro por atividade.
 * Recebido via Feign do barbershop-service (endpoint interno).
 */
public record CommissionRuleInfoDTO(
        UUID id,
        UUID activityId,
        String activityName,
        BigDecimal percentage
) {}
