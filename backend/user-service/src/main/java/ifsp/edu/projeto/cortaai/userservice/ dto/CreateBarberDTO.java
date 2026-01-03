package ifsp.edu.projeto.cortaai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import ifsp.edu.projeto.cortaai.validator.BarberDocumentCPFUnique;
import ifsp.edu.projeto.cortaai.validator.BarberEmailUnique;
import ifsp.edu.projeto.cortaai.validator.BarberTellUnique;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalTime;

@Getter
@Setter
public class CreateBarberDTO {

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
    @CPF
    private String documentCPF;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime workStartTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime workEndTime;

    @NotNull
    @Size(min = 6, max = 255) // Adicionando validação de senha
    private String password;
}