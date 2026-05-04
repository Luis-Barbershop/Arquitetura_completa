package ifsp.edu.projeto.cortaai.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ifsp.edu.projeto.cortaai.userservice.validator.CPF;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerDTO {

    private UUID id;

    @NotNull
    @Size(max = 70)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\s'.\\-]+$", message = "Nome contém caracteres inválidos")
    private String name;

    @NotNull
    @Size(max = 15)
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Formato de telefone inválido")
    private String tell;

    @NotNull
    @Size(max = 70)
    @Pattern(regexp = "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$", message = "Formato de e-mail inválido")
    private String email;

    @NotNull
    @Size(min = 11, max = 11)
    @Pattern(regexp = "^[0-9]{11}$", message = "CPF deve conter exatamente 11 dígitos numéricos")
    @CPF
    private String documentCPF;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String imageUrl;
}
