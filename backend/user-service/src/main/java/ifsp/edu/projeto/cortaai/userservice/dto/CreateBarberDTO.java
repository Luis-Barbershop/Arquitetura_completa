package ifsp.edu.projeto.cortaai.userservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateBarberDTO(
        @NotNull @Size(max = 70) String name,
        @NotNull @Size(max = 11) String phoneNumber,
        @NotNull @Size(max = 70) String email,
        @NotNull @Size(max = 11) String cpf,
        @NotNull @Size(min = 6, max = 255) String password,
        LocalTime workStartTime,
        LocalTime workEndTime
) {}


