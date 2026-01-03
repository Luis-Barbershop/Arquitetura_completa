package ifsp.edu.projeto.cortaai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CustomerDTO {

    private UUID id;

    @NotNull
    @Size(max = 70)
    private String name;

    @NotNull
    @Size(max = 11)
    // A anotação @CustomerTellUnique será mantida, mas o validador precisará ser ajustado
    private String tell;

    @NotNull
    @Size(max = 70)
    // A anotação @CustomerEmailUnique será mantida
    private String email;

    @NotNull
    @Size(max = 11)
    // A anotação @CustomerDocumentCPFUnique será mantida
    private String documentCPF;

    private String imageUrl;
}