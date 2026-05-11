package ifsp.edu.projeto.cortaai.notificationservice.feign;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Projeção do AppointmentDTO usada pelo Feign para buscar dados do barbeiro/owner.
 */
@Getter
@Setter
public class AppointmentInfoDTO {
    private UUID id;
    private UUID barberId;
    private UUID barbershopId;
    private String barberName;
    private String barbershopName;
    private BigDecimal totalPrice;
}
