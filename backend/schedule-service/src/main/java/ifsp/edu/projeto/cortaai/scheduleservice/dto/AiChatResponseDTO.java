package ifsp.edu.projeto.cortaai.scheduleservice.dto;

import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;

public record AiChatResponseDTO(
        String message,
        /** "gemini" | "groq" | "fallback" */
        String source,
        AiChatMode mode
) {}
