package ifsp.edu.projeto.cortaai.barbershop.dto;

import ifsp.edu.projeto.cortaai.barbershop.model.enums.JoinRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class JoinRequestDTO {
    private Long id;
    private UUID barberId;
    private String barberName;
    private String barberEmail;
    private JoinRequestStatus status;
    private OffsetDateTime dateCreated;
}
