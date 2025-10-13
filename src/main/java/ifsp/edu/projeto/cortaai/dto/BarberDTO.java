package ifsp.edu.projeto.cortaai.dto;

import ifsp.edu.projeto.cortaai.model.enums.BarberSkills;
import ifsp.edu.projeto.cortaai.validator.BarberDocumentCPFUnique;
import ifsp.edu.projeto.cortaai.validator.BarberEmailUnique;
import ifsp.edu.projeto.cortaai.validator.BarberTellUnique;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class BarberDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
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
