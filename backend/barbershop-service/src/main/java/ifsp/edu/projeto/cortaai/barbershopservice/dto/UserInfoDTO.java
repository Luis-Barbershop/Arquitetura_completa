package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO recebido do user-service via Feign.
 * Campos devem corresponder exatamente ao {@code UserInfoDTO} (record) do user-service.
 */
@Getter
@Setter
public class UserInfoDTO {
    private UUID id;
    private String name;
    private String email;
    private String firebaseUid;
    private String userType;
    private String role;
    private UUID barbershopId;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private String imageUrl;
}

