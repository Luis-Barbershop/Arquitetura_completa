package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_APPOINTMENTS_CONTEXT = 50;

    private final AppointmentRepository appointmentRepository;
    private final UserServiceClient userServiceClient;
    private final RestTemplate restTemplate;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String geminiUrl;

    @Value("${ai.groq.api-key:}")
    private String groqApiKey;

    @Value("${ai.groq.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqUrl;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${ai.openrouter.api-key:}")
    private String openrouterApiKey;

    @Value("${ai.openrouter.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openrouterUrl;

    @Value("${ai.openrouter.model:mistralai/mistral-7b-instruct:free}")
    private String openrouterModel;

    @Value("${ai.cohere.api-key:}")
    private String cohereApiKey;

    @Value("${ai.cohere.url:https://api.cohere.com/v2/chat}")
    private String cohereUrl;

    @Value("${ai.cohere.model:command-r}")
    private String cohereModel;

    @Override
    @Transactional(readOnly = true)
    public AiChatResponseDTO chat(String userUid, String userRole, AiChatRequestDTO request) {
        String context = buildContext(userUid, userRole, request.mode());
        String prompt  = buildPrompt(context, request.message());

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return new AiChatResponseDTO(callGemini(prompt), "gemini", request.mode());
            } catch (Exception e) {
                log.warn("gustave: Gemini indisponível — {}", e.getMessage());
            }
        }

        if (groqApiKey != null && !groqApiKey.isBlank()) {
            try {
                return new AiChatResponseDTO(callGroq(prompt), "groq", request.mode());
            } catch (Exception e) {
                log.warn("gustave: Groq indisponível — {}", e.getMessage());
            }
        }

        if (openrouterApiKey != null && !openrouterApiKey.isBlank()) {
            try {
                return new AiChatResponseDTO(callOpenRouter(prompt), "openrouter", request.mode());
            } catch (Exception e) {
                log.warn("gustave: OpenRouter indisponível — {}", e.getMessage());
            }
        }

        if (cohereApiKey != null && !cohereApiKey.isBlank()) {
            try {
                return new AiChatResponseDTO(callCohere(prompt), "cohere", request.mode());
            } catch (Exception e) {
                log.error("gustave: Cohere indisponível — {}", e.getMessage());
            }
        }

        return new AiChatResponseDTO(
                "Desculpe, o gustave está temporariamente indisponível. Tente novamente em alguns instantes.",
                "fallback",
                request.mode()
        );
    }

    // ── Construção de contexto ────────────────────────────────────────────────

    private String buildContext(String userUid, String userRole, AiChatMode mode) {
        UUID uid = UUID.fromString(userUid);
        boolean isOwner = isOwner(uid, userRole);
        LocalDateTime now = LocalDateTime.now();

        List<Appointment> appointments;

        if (mode == AiChatMode.PREVIEW) {
            appointments = isOwner
                    ? appointmentRepository.findUpcomingByBarbershop(getBarbershopId(uid), now)
                    : appointmentRepository.findUpcomingByBarberId(uid, now);
            return formatPreviewContext(appointments);
        } else {
            LocalDateTime from = now.minusDays(30);
            appointments = isOwner
                    ? appointmentRepository.findCompletedByBarbershop(getBarbershopId(uid), from, now)
                    : appointmentRepository.findCompletedByBarberId(uid, from, now);
            return formatConsolidatedContext(appointments);
        }
    }

    private boolean isOwner(UUID uid, String userRole) {
        try {
            UserInfoDTO user = userServiceClient.getUserById(uid);
            return user != null && user.getBarbershopId() != null
                    && "BARBER".equalsIgnoreCase(user.getUserType());
        } catch (Exception e) {
            log.warn("gustave: não foi possível verificar ownership para uid={}", uid);
            return false;
        }
    }

    private UUID getBarbershopId(UUID uid) {
        try {
            UserInfoDTO user = userServiceClient.getUserById(uid);
            if (user != null && user.getBarbershopId() != null) return user.getBarbershopId();
        } catch (Exception e) {
            log.warn("gustave: não foi possível obter barbershopId para uid={}", uid);
        }
        return uid; // fallback — não retorna nulo para evitar NPE
    }

    private String formatPreviewContext(List<Appointment> list) {
        if (list.isEmpty()) return "Nenhum agendamento futuro encontrado.";
        StringBuilder sb = new StringBuilder("Agendamentos futuros (até " + MAX_APPOINTMENTS_CONTEXT + " itens):\n");
        list.stream().limit(MAX_APPOINTMENTS_CONTEXT).forEach(a -> sb
                .append("- ")
                .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
                .append(" | ")
                .append(firstNameOnly(a.getCustomerName()))
                .append(" | barbeiro: ")
                .append(firstNameOnly(a.getBarberName()))
                .append(" | status: ")
                .append(a.getStatus())
                .append('\n'));
        return sb.toString();
    }

    private String formatConsolidatedContext(List<Appointment> list) {
        if (list.isEmpty()) return "Nenhum atendimento concluído nos últimos 30 dias.";
        StringBuilder sb = new StringBuilder("Atendimentos concluídos nos últimos 30 dias (até " + MAX_APPOINTMENTS_CONTEXT + " itens):\n");
        list.stream().limit(MAX_APPOINTMENTS_CONTEXT).forEach(a -> sb
                .append("- ")
                .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
                .append(" | ")
                .append(firstNameOnly(a.getCustomerName()))
                .append(" | barbeiro: ")
                .append(firstNameOnly(a.getBarberName()))
                .append(" | valor: R$ ")
                .append(a.getTotalPrice() != null ? a.getTotalPrice().toPlainString() : "0,00")
                .append('\n'));
        return sb.toString();
    }

    private String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "—";
        // Não expõe sobrenome — política de privacidade (ADR-13)
        return fullName.trim().split("\\s+")[0];
    }

    // ── Prompt ───────────────────────────────────────────────────────────────

    private String buildPrompt(String context, String message) {
        return """
                Você é o gustave, assistente de inteligência artificial do CortaAi.
                Seja direto, útil e use linguagem informal mas profissional.
                Responda sempre em português brasileiro.
                Nunca informe dados pessoais de clientes além do primeiro nome.
                Não invente dados que não estejam no contexto fornecido.

                Dados disponíveis:
                %s

                Pergunta: %s
                """.formatted(context, message);
    }

    // ── Chamadas às APIs externas ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String callGemini(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> part    = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body    = Map.of("contents", List.of(content));

        String url = geminiUrl + "?key=" + geminiApiKey;
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        Map<String, Object> first    = candidates.get(0);
        Map<String, Object> cnt      = (Map<String, Object>) first.get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) cnt.get("parts");
        return (String) parts.get(0).get("text");
    }

    @SuppressWarnings("unchecked")
    private String callGroq(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of(
                "model", groqModel,
                "messages", List.of(message),
                "max_tokens", 512
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(groqUrl, entity, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }

    @SuppressWarnings("unchecked")
    private String callOpenRouter(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openrouterApiKey != null ? openrouterApiKey : "");
        headers.set("HTTP-Referer", "https://cortaai.shop");
        headers.set("X-Title", "CortaAi — gustave");

        Map<String, Object> message = Map.of("role", "user", "content", prompt);
        Map<String, Object> body = Map.of(
                "model", openrouterModel,
                "messages", List.of(message),
                "max_tokens", 512
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(openrouterUrl != null ? openrouterUrl : "", entity, Map.class);
        if (response == null) throw new IllegalStateException("Resposta nula do OpenRouter");

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        return (String) msg.get("content");
    }

    @SuppressWarnings("unchecked")
    private String callCohere(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(cohereApiKey != null ? cohereApiKey : "");

        Map<String, Object> body = Map.of(
                "model", cohereModel,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(cohereUrl != null ? cohereUrl : "", entity, Map.class);
        if (response == null) throw new IllegalStateException("Resposta nula do Cohere");

        // Cohere v2: response.message.content[0].text
        Map<String, Object> msgObj = (Map<String, Object>) response.get("message");
        List<Map<String, Object>> content = (List<Map<String, Object>>) msgObj.get("content");
        return (String) content.get(0).get("text");
    }
}
