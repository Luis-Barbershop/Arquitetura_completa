package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateBarberDTO {
    @NotBlank(message = "O nome é obrigatório.")
    private String name;

    @NotBlank(message = "O telefone é obrigatório.")
    private String phoneNumber;

    @Email(message = "Email inválido.")
    private String email;

    // Adicione senha se for permitir alterar por aqui
}