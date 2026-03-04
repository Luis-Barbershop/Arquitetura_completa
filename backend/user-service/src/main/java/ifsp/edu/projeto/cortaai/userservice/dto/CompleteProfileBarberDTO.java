package ifsp.edu.projeto.cortaai.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Dados complementares enviados por um barbeiro após login social.
 *
 * <p>Análogo ao {@link CompleteProfileCustomerDTO}, mas inclui também
 * os horários de trabalho necessários para agendamentos.
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

        @NotNull(message = "Horário de início é obrigatório")
        @JsonFormat(pattern = "HH:mm")
        LocalTime workStartTime,

        @NotNull(message = "Horário de término é obrigatório")
        @JsonFormat(pattern = "HH:mm")
        LocalTime workEndTime,

        /** Indica se este barbeiro também é dono da barbearia. */
        boolean isOwner
) {}
