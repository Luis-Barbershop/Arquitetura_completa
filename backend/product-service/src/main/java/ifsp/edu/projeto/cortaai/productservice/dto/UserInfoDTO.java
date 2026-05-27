package ifsp.edu.projeto.cortaai.productservice.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoDTO {
    private UUID id;
    private String name;
    private String email;
    private String userType;
    private String role;
    private UUID barbershopId;
}
