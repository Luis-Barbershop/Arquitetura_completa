package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AiChatService;
import ifsp.edu.projeto.cortaai.scheduleservice.service.ChatHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final ChatHistoryService chatHistoryService;

    /**
     * POST /api/schedule/ai/chat
     * Headers injetados pelo api-gateway: X-User-Id, X-User-Role
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(
            @RequestHeader("X-User-UID")   String userUid,
            @RequestHeader("X-User-Type") String userRole,
            @Valid @RequestBody AiChatRequestDTO request) {

        AiChatResponseDTO response = aiChatService.chat(userUid, userRole, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/schedule/ai/chat/history
     * Limpa o histórico do Gustavo para o usuário — chamado no logout.
     */
    @DeleteMapping("/chat/history")
    public ResponseEntity<Void> clearHistory(
            @RequestHeader("X-User-UID") String userUid) {
        chatHistoryService.clearHistory(userUid);
        return ResponseEntity.noContent().build();
    }
}
