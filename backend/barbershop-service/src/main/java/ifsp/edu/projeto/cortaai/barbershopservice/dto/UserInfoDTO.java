package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO recebido do user-service via Feign.
 */
@Getter
@Setter
public class UserInfoDTO {
    private UUID id;
    private String name;
    private String email;
    private String userType;
    private String role;
    private UUID barbershopId;
    private String imageUrl;
}

