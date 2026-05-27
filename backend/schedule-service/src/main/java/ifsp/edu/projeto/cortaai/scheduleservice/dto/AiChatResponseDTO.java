package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;

public record AiChatResponseDTO(
        String message,
        /** "openrouter" | "cohere" | "fallback" */
        String source,
        AiChatMode mode
) {}
