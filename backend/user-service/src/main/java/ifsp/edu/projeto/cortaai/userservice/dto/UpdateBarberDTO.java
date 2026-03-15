package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBarberDTO {
    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 70)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.\\-]+$", message = "Nome contém caracteres inválidos")
    private String name;

    @NotBlank(message = "O telefone é obrigatório.")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
    private String phoneNumber;

    @Email(message = "Email inválido.")
    @Size(max = 70)
    private String email;
}

