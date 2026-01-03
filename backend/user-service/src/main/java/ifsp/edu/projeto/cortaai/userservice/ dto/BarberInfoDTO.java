package ifsp.edu.projeto.cortaai.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class BarberInfoDTO {
    private UUID id;
    private String name;
    private String email;
    private String tell;
}