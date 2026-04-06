package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar um novo usuário no Firebase Authentication via e-mail/senha
 * e já completar o perfil no backend em uma única chamada de teste.
 *
 * <p>Usado exclusivamente pelo {@code FirebaseTestController} (Swagger / debug).
 */
public record FirebaseEmailRegisterRequestDTO(

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email inválido")
        String email,

        @NotBlank(message = "password é obrigatório")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
        @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
            message = "A senha deve conter pelo menos 1 letra maiúscula, 1 número e 1 caractere especial."
        )
        String password,

        /**
         * Tipo do usuário: {@code CUSTOMER} ou {@code BARBER}.
         * Determina qual endpoint de complete-profile será chamado.
         */
        @NotBlank(message = "userType é obrigatório (CUSTOMER ou BARBER)")
        String userType,

        // ── Campos comuns ──────────────────────────────────────────────────────
        @NotBlank(message = "name é obrigatório")
        String name,

        @NotBlank(message = "tell é obrigatório")
        String tell,

        @NotBlank(message = "documentCPF é obrigatório")
        String documentCPF,

        // ── Campos exclusivos de BARBER (ignorados para CUSTOMER) ──────────────
        /** Horário de início no formato {@code HH:mm} (ex.: "09:00"). Obrigatório para BARBER. */
        String workStartTime,

        /** Horário de término no formato {@code HH:mm} (ex.: "18:00"). Obrigatório para BARBER. */
        String workEndTime,

        /** Se o barbeiro é também dono da barbearia. Default {@code false}. */
        Boolean isOwner
) {}
