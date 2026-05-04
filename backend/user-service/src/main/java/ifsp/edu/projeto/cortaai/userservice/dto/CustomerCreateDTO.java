package ifsp.edu.projeto.cortaai.userservice.dto;

import ifsp.edu.projeto.cortaai.userservice.validator.CustomerDocumentCPFUnique;
import ifsp.edu.projeto.cortaai.userservice.validator.CustomerEmailUnique;
import ifsp.edu.projeto.cortaai.userservice.validator.CustomerTellUnique;
import ifsp.edu.projeto.cortaai.userservice.validator.CPF;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerCreateDTO {

    @NotNull
    @Size(max = 70)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.\\-]+$", message = "Nome contém caracteres inválidos")
    private String name;

    @NotNull
    @Size(max = 15)
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
    @CustomerTellUnique
    private String tell;

    @NotNull
    @Size(max = 70)
    @Pattern(regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$", message = "Formato de e-mail inválido")
    @CustomerEmailUnique
    private String email;

    @NotNull
    @Size(min = 11, max = 11)
    @Pattern(regexp = "^[0-9]{11}$", message = "CPF deve conter exatamente 11 dígitos numéricos")
    @CustomerDocumentCPFUnique
    @CPF
    private String documentCPF;

    @NotNull
    @Size(max = 70)
    private String password;
}
