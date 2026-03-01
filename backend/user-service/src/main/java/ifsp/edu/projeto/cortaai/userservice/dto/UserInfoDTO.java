package ifsp.edu.projeto.cortaai.userservice.dto;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO para comunicação inter-serviço.
 * Contém informações essenciais do usuário para outros microserviços.
 */
public record UserInfoDTO(
        UUID id,
        String name,
        String email,
        String userType,
        String role,
        UUID barbershopId,
        LocalTime workStartTime,
        LocalTime workEndTime,
        String imageUrl
) {}

