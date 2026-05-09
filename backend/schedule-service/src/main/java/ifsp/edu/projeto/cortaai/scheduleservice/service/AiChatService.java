package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;

public interface AiChatService {
    AiChatResponseDTO chat(String userUid, String userRole, AiChatRequestDTO request);
}
