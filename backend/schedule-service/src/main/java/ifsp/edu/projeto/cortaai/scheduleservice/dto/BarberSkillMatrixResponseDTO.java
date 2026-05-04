package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import java.math.BigDecimal;

public record BarberSkillMatrixResponseDTO(
        String barberId,
        String barberName,
        String activityName,
        Long timesExecuted,
        BigDecimal totalGeneratedByActivity
) {}
