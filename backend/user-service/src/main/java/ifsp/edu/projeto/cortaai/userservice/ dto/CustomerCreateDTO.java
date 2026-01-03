package ifsp.edu.projeto.cortaai.userservice.dto;
import ifsp.edu.projeto.cortaai.userservice.validator.CustomerDocumentCPFUnique;
import ifsp.edu.projeto.cortaai.userservice.validator.CustomerEmailUnique;
import ifsp.edu.projeto.cortaai.userservice.validator.CustomerTellUnique;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;


@Getter
@Setter
public class CustomerCreateDTO {

    @NotNull
    @Size(max = 70)
    private String name;

    @NotNull
    @Size(max = 11)
    @CustomerTellUnique
    private String tell;

    @NotNull
    @Size(max = 70)
    @CustomerEmailUnique
    private String email;

    @NotNull
    @Size(max = 11)
    @CustomerDocumentCPFUnique
    @CPF
    private String documentCPF;

    @NotNull
    @Size(max = 70)
    private String password;

}
