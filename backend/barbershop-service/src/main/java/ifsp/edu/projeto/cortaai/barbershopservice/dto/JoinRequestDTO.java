package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinRequestDTO {
    private UUID requestId;
    private UUID barberId;
    private String barberName;
    private String barberEmail;
    private String status;
}

