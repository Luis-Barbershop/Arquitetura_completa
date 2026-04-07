package ifsp.edu.projeto.cortaai.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados complementares enviados por um barbeiro após login social ou cadastro e-mail.
 * Horários de expediente são configurados posteriormente no perfil.
 */
public record CompleteProfileBarberDTO(

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
        String tell,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 14, message = "CPF deve ter entre 11 e 14 caracteres")
        String documentCPF,

        /** Nome de exibição (opcional). */
        String name,

        @NotNull(message = "Data de nascimento é obrigatória")
        @Past(message = "Data de nascimento deve ser no passado")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate birthDate,

        /** Indica se este barbeiro também é dono da barbearia. */
        boolean isOwner
) {}
