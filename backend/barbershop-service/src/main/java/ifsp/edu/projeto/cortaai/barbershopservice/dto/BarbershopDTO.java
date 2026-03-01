package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BarbershopDTO {
    private UUID id;
    private UUID ownerId;
    private String name;
    private String cnpj;
    private String address;
    private String logoUrl;
    private String bannerUrl;
    private List<String> highlightUrls;
}

