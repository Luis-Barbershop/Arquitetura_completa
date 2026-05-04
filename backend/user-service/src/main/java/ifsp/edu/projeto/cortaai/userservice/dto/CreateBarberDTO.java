package ifsp.edu.projeto.cortaai.userservice.dto;

import ifsp.edu.projeto.cortaai.userservice.validator.CPF;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateBarberDTO(
        @NotNull @Size(max = 70) String name,
        @NotNull @Size(max = 11) String phoneNumber,
        @NotNull @Size(max = 70) String email,
        @NotNull
        @Size(min = 11, max = 11)
        @Pattern(regexp = "^[0-9]{11}$", message = "CPF deve conter exatamente 11 dígitos numéricos")
        @CPF
        String cpf,
        @NotNull @Size(min = 6, max = 255) String password,
        LocalTime workStartTime,
        LocalTime workEndTime
) {}

