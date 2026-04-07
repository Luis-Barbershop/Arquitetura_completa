package ifsp.edu.projeto.cortaai.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO para atualização parcial do perfil de um barbeiro.
 * Todos os campos são opcionais (patch-like).
 * Os horários de expediente só podem ser alterados se o barbeiro
 * estiver vinculado a uma barbearia (validação feita no service/frontend).
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

    @Past(message = "Data de nascimento deve ser no passado")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /** Horário de início do expediente — editável apenas com barbearia vinculada. */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime workStartTime;

    /** Horário de término do expediente — editável apenas com barbearia vinculada. */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime workEndTime;

    /**
     * Se o dono do estabelecimento deseja aparecer como barbeiro disponível
     * para agendamentos. Ignorado para barbeiros não-owners (sempre true).
     */
    private Boolean actAsBarber;
}

