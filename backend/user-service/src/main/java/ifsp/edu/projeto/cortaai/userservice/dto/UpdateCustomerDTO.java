package ifsp.edu.projeto.cortaai.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO para atualização parcial do perfil de um cliente.
 * Todos os campos são opcionais (patch-like).
 */
@Data
public class UpdateCustomerDTO {

    @Size(max = 70)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.\\-]+$", message = "Nome contém caracteres inválidos")
    private String name;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
    private String tell;

    @Email(message = "Email inválido.")
    @Size(max = 70)
    private String email;

    @Past(message = "Data de nascimento deve ser no passado")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
}
