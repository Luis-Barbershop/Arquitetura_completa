package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiChatRequestDTO(
        @NotBlank String message,
        @NotNull  AiChatMode mode
) {}
