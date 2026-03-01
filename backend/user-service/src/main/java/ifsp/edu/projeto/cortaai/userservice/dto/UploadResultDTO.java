package ifsp.edu.projeto.cortaai.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO para encapsular o resultado de um upload de arquivo.
 */
@Data
@AllArgsConstructor
public class UploadResultDTO {
    private String publicId;
    private String secureUrl;
}

