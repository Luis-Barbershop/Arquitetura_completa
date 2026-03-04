package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados complementares enviados por um cliente (customer) após login social.
 *
 * <p>No login com Google/Apple/etc., o Firebase não fornece CPF nem telefone.
 * O app deve chamar {@code POST /api/auth/customers/complete-profile} com este
 * DTO para completar o perfil antes de usar funcionalidades que exigem esses dados.
 */
public record CompleteProfileCustomerDTO(

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
        String tell,

        @NotBlank(message = "CPF é obrigatório")
        @Size(min = 11, max = 14, message = "CPF deve ter entre 11 e 14 caracteres")
        String documentCPF,

        /** Nome de exibição (opcional — substitui o nome vindo do Firebase se fornecido). */
        String name
) {}
