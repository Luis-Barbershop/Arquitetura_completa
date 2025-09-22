package ifsp.edu.projeto.cortaai.customer;

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
    @CustomerTellUnique
    private String tell;

    @NotNull
    @Size(max = 70)
    @CustomerEmailUnique
    private String email;

    @NotNull
    @Size(max = 11)
    @CustomerDocumentCPFUnique
    private String documentCPF;

    @NotNull
    @Size(max = 70)
    private String password;

    @Size(max = 255)
    @CustomerPreviousAppointmentUnique
    private String previousAppointment;

}
