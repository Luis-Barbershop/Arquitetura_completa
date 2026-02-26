package ifsp.edu.projeto.cortaai.barbershop.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BarbershopDTO {
    private UUID id;
    private String name;
    private String cnpj;
    private String address;
    private UUID ownerId;
    private String logoUrl;
    private String bannerUrl;
    private OffsetDateTime dateCreated;
    private OffsetDateTime lastUpdated;
    private List<String> highlightUrls;
}
