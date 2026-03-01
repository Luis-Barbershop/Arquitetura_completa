package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class BarbershopInfoDTO implements Serializable {
    private UUID id;
    private UUID ownerId;
    private String name;
    private String cnpj;
    private String address;
}

