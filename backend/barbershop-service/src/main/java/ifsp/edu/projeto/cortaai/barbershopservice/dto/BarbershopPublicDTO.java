package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BarbershopPublicDTO {
    private UUID id;
    private String name;
    private String address;
    private String logoUrl;
    private String bannerUrl;
    private Double averageRating;
    private Long reviewsCount;
    private Double latitude;
    private Double longitude;
    private List<String> highlightUrls;
}
