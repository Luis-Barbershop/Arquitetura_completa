package ifsp.edu.projeto.cortaai.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateAppointmentDTO {

    @NotNull(message = "Barbearia é obrigatória")
    private UUID barbershopId;

    @NotNull(message = "Barbeiro é obrigatório")
    private UUID barberId;

    @NotNull(message = "Horário de início é obrigatório")
    private OffsetDateTime startTime;

    @NotEmpty(message = "Pelo menos um serviço deve ser selecionado")
    private List<UUID> activityIds;

    private String notes;
}
