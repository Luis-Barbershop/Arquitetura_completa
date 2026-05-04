package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import ifsp.edu.projeto.cortaai.barbershopservice.validator.CPF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO enviado pelo owner ao convidar um barbeiro via CPF.
 */
@Getter
@Setter
public class InviteBarberDTO {
    @NotBlank(message = "O CPF é obrigatório.")
    @Size(min = 11, max = 14, message = "CPF deve ter entre 11 e 14 caracteres")
    @CPF
    private String cpf;
}
