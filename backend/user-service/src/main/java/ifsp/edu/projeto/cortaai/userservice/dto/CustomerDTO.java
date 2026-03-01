package ifsp.edu.projeto.cortaai.userservice.dto;

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
    private String tell;

    @NotNull
    @Size(max = 70)
    private String email;

    @NotNull
    @Size(max = 11)
    private String documentCPF;

    private String imageUrl;
}

