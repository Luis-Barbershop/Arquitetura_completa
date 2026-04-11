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
    /** Tipo: JOIN (barbeiro pediu) ou INVITE (owner convidou). */
    private String requestType;
    /** UUID da barbearia — útil para o barbeiro saber de qual barbearia é o convite. */
    private UUID barbershopId;
    /** Nome da barbearia — exibido ao barbeiro na tela de convites pendentes. */
    private String barbershopName;
}

