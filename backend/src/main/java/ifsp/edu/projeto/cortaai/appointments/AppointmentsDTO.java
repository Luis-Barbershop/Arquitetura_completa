package ifsp.edu.projeto.cortaai.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AppointmentsDTO {

    private Long id;
    private OffsetDateTime datetime;
    private UUID barber;
    private UUID customer;

}
