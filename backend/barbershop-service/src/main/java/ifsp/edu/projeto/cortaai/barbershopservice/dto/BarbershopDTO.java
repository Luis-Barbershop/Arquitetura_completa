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
    private Double averageRating;
    private Long reviewsCount;
    private List<String> highlightUrls;
}

