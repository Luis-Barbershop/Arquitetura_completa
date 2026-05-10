package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.service.ChatHistoryService;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.PaymentServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VBarberSkillMatrixRepository;
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
    private final VBarberSkillMatrixRepository vBarberSkillMatrixRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final ChatHistoryService chatHistoryService;
    private final RestTemplate restTemplate;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
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
        // Resolve dados do usuário uma vez e repassa para contexto e prompt
        UserInfoDTO resolvedUser = null;
        try {
            resolvedUser = userServiceClient.getUserByFirebaseUid(userUid);
        } catch (Exception e) {
            log.warn("gustavo: não foi possível resolver usuário firebaseUid={}", userUid);
        }

        // Busca histórico da sessão no Redis antes de montar o prompt
        var history = chatHistoryService.getHistory(userUid);

        String context = buildContext(userUid, userRole, request.mode(), resolvedUser);
        String prompt  = buildPrompt(context, chatHistoryService.formatHistoryForPrompt(history), request.message(), resolvedUser);

        String reply = null;

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try { reply = callGemini(prompt); } catch (Exception e) {
                log.warn("gustavo: Gemini indisponível — {}", e.getMessage());
            }
        }
        if (reply == null && groqApiKey != null && !groqApiKey.isBlank()) {
            try { reply = callGroq(prompt); } catch (Exception e) {
                log.warn("gustavo: Groq indisponível — {}", e.getMessage());
            }
        }
        if (reply == null && openrouterApiKey != null && !openrouterApiKey.isBlank()) {
            try { reply = callOpenRouter(prompt); } catch (Exception e) {
                log.warn("gustavo: OpenRouter indisponível — {}", e.getMessage());
            }
        }
        if (reply == null && cohereApiKey != null && !cohereApiKey.isBlank()) {
            try { reply = callCohere(prompt); } catch (Exception e) {
                log.error("gustavo: Cohere indisponível — {}", e.getMessage());
            }
        }

        if (reply == null) {
            reply = "Desculpe, o Gustavo está temporariamente indisponível. Tente novamente em alguns instantes.";
            return new AiChatResponseDTO(reply, "fallback", request.mode());
        }

        // Persiste o turno no Redis para contexto futuro
        chatHistoryService.appendTurn(userUid, request.message(), reply);

        String source = geminiApiKey != null && !geminiApiKey.isBlank() ? "gemini"
                : groqApiKey != null && !groqApiKey.isBlank() ? "groq"
                : openrouterApiKey != null && !openrouterApiKey.isBlank() ? "openrouter" : "cohere";

        return new AiChatResponseDTO(reply, source, request.mode());
    }
    // ── Construção de contexto ────────────────────────────────────────────────

    private String buildContext(String firebaseUid, String userRole, AiChatMode mode, UserInfoDTO user) {
        // user já resolvido pelo chamador via getUserByFirebaseUid

        UUID internalId   = user != null ? user.getId() : null;
        UUID barbershopId = user != null ? user.getBarbershopId() : null;
        boolean isCustomer = user != null && "CUSTOMER".equalsIgnoreCase(user.getUserType());
        boolean isOwner    = !isCustomer && barbershopId != null;
        LocalDateTime now  = LocalDateTime.now();

        StringBuilder ctx = new StringBuilder();

        // 1. Agenda (sempre incluída — requer internalId válido)
        if (internalId != null) {
            List<Appointment> appointments;
            if (isCustomer) {
                // Cliente: busca pelos seus próprios agendamentos via customer_id
                if (mode == AiChatMode.PREVIEW) {
                    appointments = appointmentRepository.findUpcomingByCustomerId(internalId, now);
                    ctx.append(formatPreviewContextCustomer(appointments));
                } else {
                    LocalDateTime from = now.minusDays(90);
                    appointments = appointmentRepository.findCompletedByCustomerId(internalId, from, now);
                    ctx.append(formatConsolidatedContextCustomer(appointments));
                    // Cancelados do cliente (últimos 90 dias)
                    List<Appointment> cancelados = appointmentRepository.findCancelledByCustomerId(internalId, from, now);
                    if (!cancelados.isEmpty()) {
                        ctx.append('\n').append(formatCancelledContext(cancelados));
                    }
                }
            } else if (mode == AiChatMode.PREVIEW) {
                appointments = isOwner
                        ? appointmentRepository.findUpcomingByBarbershop(barbershopId, now)
                        : appointmentRepository.findUpcomingByBarberId(internalId, now);
                ctx.append(formatPreviewContext(appointments));
            } else {
                LocalDateTime from = now.minusDays(90);
                appointments = isOwner
                        ? appointmentRepository.findCompletedByBarbershop(barbershopId, from, now)
                        : appointmentRepository.findCompletedByBarberId(internalId, from, now);
                ctx.append(formatConsolidatedContext(appointments));
                // Cancelados da barbearia (últimos 90 dias) — ajuda o dono a entender evasão
                if (isOwner) {
                    List<Appointment> cancelados = appointmentRepository.findCancelledByBarbershop(barbershopId, from, now);
                    if (!cancelados.isEmpty()) {
                        ctx.append('\n').append(formatCancelledContext(cancelados));
                    }
                }
            }
        } else {
            ctx.append("Dados de agenda não disponíveis no momento.");
        }

        // 2–4. Contexto adicional exclusivo para owners
        if (isOwner) {
            ctx.append("\n\n").append(formatSkillMatrix(barbershopId));
            ctx.append("\n\n").append(formatStockContext(barbershopId));
            ctx.append("\n\n").append(formatFinancialContext(barbershopId));
        }

        return ctx.toString();
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

    private String formatPreviewContextCustomer(List<Appointment> list) {
        if (list.isEmpty()) return "Você não possui agendamentos futuros.";
        StringBuilder sb = new StringBuilder("Seus próximos agendamentos:\n");
        list.stream().limit(MAX_APPOINTMENTS_CONTEXT).forEach(a -> {
            String servicos = a.getActivities() == null || a.getActivities().isEmpty() ? "—"
                    : a.getActivities().stream().map(act -> act.getActivityName()).collect(java.util.stream.Collectors.joining(", "));
            sb.append("- ")
              .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
              .append(" | barbeiro: ").append(firstNameOnly(a.getBarberName()))
              .append(" | serviço: ").append(servicos)
              .append(" | status: ").append(a.getStatus())
              .append('\n');
        });
        return sb.toString();
    }

    private String formatConsolidatedContext(List<Appointment> list) {
        if (list.isEmpty()) return "Nenhum atendimento concluído nos últimos 90 dias.";
        StringBuilder sb = new StringBuilder("Atendimentos concluídos nos últimos 90 dias (até " + MAX_APPOINTMENTS_CONTEXT + " itens):\n");
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

    private String formatConsolidatedContextCustomer(List<Appointment> list) {
        if (list.isEmpty()) return "Nenhum atendimento concluído nos últimos 90 dias.";
        StringBuilder sb = new StringBuilder("Seus atendimentos concluídos nos últimos 90 dias:\n");
        list.stream().limit(MAX_APPOINTMENTS_CONTEXT).forEach(a -> {
            String servicos = a.getActivities() == null || a.getActivities().isEmpty() ? "—"
                    : a.getActivities().stream().map(act -> act.getActivityName()).collect(java.util.stream.Collectors.joining(", "));
            sb.append("- ")
              .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
              .append(" | barbeiro: ").append(firstNameOnly(a.getBarberName()))
              .append(" | serviço: ").append(servicos)
              .append(" | valor: R$ ")
              .append(a.getTotalPrice() != null ? a.getTotalPrice().toPlainString() : "0,00")
              .append('\n');
        });
        return sb.toString();
    }

    private String formatCancelledContext(List<Appointment> list) {
        StringBuilder sb = new StringBuilder("Agendamentos cancelados / não compareceu:\n");
        list.stream().limit(20).forEach(a -> sb
                .append("- ")
                .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
                .append(" | status: ").append(a.getStatus())
                .append('\n'));
        return sb.toString();
    }

    private String firstNameOnly(String fullName) {
        if (fullName == null || fullName.isBlank()) return "—";
        // Não expõe sobrenome — política de privacidade (ADR-13)
        return fullName.trim().split("\\s+")[0];
    }

    private String formatSkillMatrix(UUID barbershopId) {
        try {
            var rows = vBarberSkillMatrixRepository.findByBarbershopId(barbershopId.toString());
            if (rows.isEmpty()) return "Habilidades dos barbeiros: sem dados disponíveis.";
            StringBuilder sb = new StringBuilder("Habilidades e serviços executados por barbeiro:\n");
            rows.forEach(r -> sb
                    .append("- ")
                    .append(firstNameOnly(r.getBarberName()))
                    .append(" | serviço: ").append(r.getActivityName())
                    .append(" | vezes executado: ").append(r.getTimesExecuted())
                    .append('\n'));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter skill matrix — {}", e.getMessage());
            return "Habilidades dos barbeiros: dados temporariamente indisponíveis.";
        }
    }

    private String formatStockContext(UUID barbershopId) {
        try {
            List<Map<String, Object>> items = productServiceClient.getStockHealth(barbershopId);
            if (items == null || items.isEmpty()) return "Estoque: sem alertas de reposição no momento.";
            StringBuilder sb = new StringBuilder("Situação do estoque:\n");
            items.forEach(item -> sb
                    .append("- ").append(item.getOrDefault("productName", "?"))
                    .append(" | categoria: ").append(item.getOrDefault("category", "?"))
                    .append(" | qtd atual: ").append(item.getOrDefault("currentStock", "?"))
                    .append(" | mínimo: ").append(item.getOrDefault("predictedMinimum", "?"))
                    .append(" | repor: ").append(Boolean.TRUE.equals(item.get("requiresRestock")) ? "SIM" : "não")
                    .append('\n'));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter estoque — {}", e.getMessage());
            return "Estoque: dados temporariamente indisponíveis.";
        }
    }

    private String formatFinancialContext(UUID barbershopId) {
        try {
            List<Map<String, Object>> perfs = paymentServiceClient.getBarberPerformance(barbershopId);
            if (perfs == null || perfs.isEmpty()) return "Financeiro: sem dados de performance disponíveis.";
            StringBuilder sb = new StringBuilder("Performance financeira dos barbeiros:\n");
            perfs.forEach(p -> sb
                    .append("- ").append(p.getOrDefault("barberName", "?"))
                    .append(" | atendimentos: ").append(p.getOrDefault("totalAppointments", "?"))
                    .append(" | receita gerada: R$ ").append(p.getOrDefault("generatedRevenue", "0"))
                    .append(" | participação: ").append(p.getOrDefault("contributionPercentage", "0")).append("%")
                    .append('\n'));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter financeiro — {}", e.getMessage());
            return "Financeiro: dados temporariamente indisponíveis.";
        }
    }

    // ── Prompt ───────────────────────────────────────────────────────────────

    private String buildPrompt(String context, String history, String message, UserInfoDTO user) {
        String nomeUsuario = user != null && user.getName() != null
                ? user.getName().trim().split("\\s+")[0]
                : "usuário";
        boolean isOwner = user != null && user.getBarbershopId() != null
                && "BARBER".equalsIgnoreCase(user.getUserType());
        String perfil = isOwner ? "DONO DE BARBEARIA" : "BARBEIRO COLABORADOR";
        String historyBlock = history.isBlank() ? "" : "\n" + history + "\n";

        return """
                Você é o Gustavo, assistente de IA do CortaAi. Seu único objetivo é analisar os dados reais do sistema e responder perguntas de gestão de barbearia.

                PERFIL DO USUÁRIO LOGADO:
                - Nome: %s
                - Tipo: %s
                %s

                DADOS REAIS DO SISTEMA (extraídos agora para este usuário):
                %s
                %s
                REGRAS OBRIGATÓRIAS:
                1. Baseie TODA resposta exclusivamente nos dados acima. NUNCA invente, estime ou suponha valores.
                2. Se a informação não estiver nos dados acima, responda: "Não encontrei esse dado no seu painel agora."
                3. Responda em português brasileiro, de forma direta e sem introduções desnecessárias (não comece com "Claro!", "Olá!", "Com certeza!" etc.).
                4. Seja conciso: vá direto ao ponto. Use listas apenas quando houver múltiplos itens.
                5. Se a pergunta for fora do contexto de gestão de barbearia (agenda, financeiro, equipe, estoque), recuse: "Meu foco é a gestão da sua barbearia. Posso ajudar com agenda, financeiro, equipe ou estoque."
                6. Nunca exponha sobrenomes ou dados pessoais de clientes.
                7. Use o histórico da conversa acima para manter continuidade — se o usuário disser "ele" ou "aquele", interprete com base no contexto anterior.
                %s

                Pergunta: %s
                """.formatted(
                nomeUsuario,
                perfil,
                isOwner
                        ? "- Acesso: agenda completa da barbearia, financeiro, estoque e equipe"
                        : "- Acesso: apenas seus próprios agendamentos",
                context,
                historyBlock,
                isOwner
                        ? ""
                        : "8. Este usuário é colaborador, não dono. Não forneça dados financeiros globais da barbearia, apenas os dados dele.",
                message
        );
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
