package ifsp.edu.projeto.cortaai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private Object userData; // Usamos Object para poder retornar BarberDTO ou CustomerDTO
}