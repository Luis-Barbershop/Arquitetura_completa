package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CreateBarberBlockDTO {

    @NotNull
    private UUID barberId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @Size(max = 255)
    @Pattern(regexp = "^[\\p{L}\\p{M}\\p{N}\\s,.!?\\-]*$", message = "Motivo contém caracteres inválidos")
    private String reason;
}

