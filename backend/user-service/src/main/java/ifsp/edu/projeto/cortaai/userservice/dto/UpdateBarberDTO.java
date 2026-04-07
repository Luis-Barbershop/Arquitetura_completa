package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para atualização parcial do perfil de um barbeiro.
 * Todos os campos são opcionais (patch-like).
 */
@Data
public class UpdateBarberDTO {

    @Size(max = 70)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.\\-]+$", message = "Nome contém caracteres inválidos")
    private String name;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
    private String tell;

    @Email(message = "Email inválido.")
    @Size(max = 70)
    private String email;

    /**
     * Se o dono do estabelecimento deseja aparecer como barbeiro disponível
     * para agendamentos. Ignorado para barbeiros não-owners (sempre true).
     */
    private Boolean actAsBarber;
}

