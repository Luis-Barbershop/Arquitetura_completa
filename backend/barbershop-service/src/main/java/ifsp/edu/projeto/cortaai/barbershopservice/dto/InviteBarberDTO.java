package ifsp.edu.projeto.cortaai.barbershopservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO enviado pelo owner ao convidar um barbeiro via CPF.
 */
@Getter
@Setter
public class InviteBarberDTO {
    @NotBlank(message = "O CPF é obrigatório.")
    private String cpf;
}
