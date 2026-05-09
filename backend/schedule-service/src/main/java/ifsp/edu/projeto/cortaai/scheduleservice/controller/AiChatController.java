package ifsp.edu.projeto.cortaai.scheduleservice.controller;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.service.AiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    /**
     * POST /api/schedule/ai/chat
     * Headers injetados pelo api-gateway: X-User-Id, X-User-Role
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(
            @RequestHeader("X-User-Id")   String userUid,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody AiChatRequestDTO request) {

        AiChatResponseDTO response = aiChatService.chat(userUid, userRole, request);
        return ResponseEntity.ok(response);
    }
}
