package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResultDTO {
    private String publicId;
    private String secureUrl;
}

