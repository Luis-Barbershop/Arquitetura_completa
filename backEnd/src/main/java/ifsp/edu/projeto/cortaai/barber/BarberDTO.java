package ifsp.edu.projeto.cortaai.barber;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BarberDTO {

    private UUID id;

    @NotNull
    @Size(max = 70)
    private String name;

    @NotNull
    @Size(max = 11)
    @BarberTellUnique
    private String tell;

    @NotNull
    @Size(max = 70)
    @BarberEmailUnique
    private String email;

    @NotNull
    @Size(max = 11)
    @BarberDocumentCPFUnique
    private String documentCPF;

    @NotNull
    private BarberSkills mainSkill;

    private BarberSkills secondSkill;

    private BarberSkills thirdSkill;

    @NotNull
    private UUID barberShop;

}
