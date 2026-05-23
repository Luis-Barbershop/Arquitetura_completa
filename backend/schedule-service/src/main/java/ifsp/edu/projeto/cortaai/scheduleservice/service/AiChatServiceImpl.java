package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.service.ChatHistoryService;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CommissionRuleInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_APPOINTMENTS_CONTEXT = 50;

    private final AppointmentRepository appointmentRepository;
    private final VBarberSkillMatrixRepository vBarberSkillMatrixRepository;
    private final UserServiceClient userServiceClient;
    private final BarbershopServiceClient barbershopServiceClient;
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
        String prompt  = buildPrompt(context, chatHistoryService.formatHistoryForPrompt(history), request.message(), resolvedUser, userRole);

        String reply = null;
        String source = "fallback";

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try { reply = callGemini(prompt); source = "gemini"; } catch (Exception e) {
                log.warn("gustavo: Gemini indisponível — {}", e.getMessage());
            }
        }
        if (reply == null && groqApiKey != null && !groqApiKey.isBlank()) {
            try { reply = callGroq(prompt); source = "groq"; } catch (Exception e) {
                log.warn("gustavo: Groq indisponível — {}", e.getMessage());
            }
        }
        if (reply == null && openrouterApiKey != null && !openrouterApiKey.isBlank()) {
            try { reply = callOpenRouter(prompt); source = "openrouter"; } catch (Exception e) {
                log.warn("gustavo: OpenRouter indisponível — {}", e.getMessage());
            }
        }
        if (reply == null && cohereApiKey != null && !cohereApiKey.isBlank()) {
            try { reply = callCohere(prompt); source = "cohere"; } catch (Exception e) {
                log.error("gustavo: Cohere indisponível — {}", e.getMessage());
            }
        }

        if (reply == null) {
            reply = "Desculpe, o Gustavo está temporariamente indisponível. Tente novamente em alguns instantes.";
            return new AiChatResponseDTO(reply, "fallback", request.mode());
        }

        // Persiste o turno no Redis para contexto futuro
        chatHistoryService.appendTurn(userUid, request.message(), reply);

        return new AiChatResponseDTO(reply, source, request.mode());
    }
    // ── Construção de contexto ────────────────────────────────────────────────

    private String buildContext(String firebaseUid, String userRole, AiChatMode mode, UserInfoDTO user) {
        // user já resolvido pelo chamador via getUserByFirebaseUid

        UUID internalId   = user != null ? user.getId() : null;
        UUID barbershopId = user != null ? user.getBarbershopId() : null;
        boolean isCustomer = user != null && "CUSTOMER".equalsIgnoreCase(user.getUserType());
        boolean isOwner    = isOwnerUser(user, userRole);
        LocalDateTime now  = LocalDateTime.now();
        LocalDate today = LocalDate.now();

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

        if (!isCustomer && internalId != null) {
            ctx.append("\n\n").append(formatOperationalAppointmentMetrics(internalId, barbershopId, isOwner, today, now));
        }

        if (!isCustomer && barbershopId != null) {
            ctx.append("\n\n").append(formatDashboardFinancialContext(firebaseUid, barbershopId, internalId, isOwner, today, now));
        }

        // 2–4. Contexto adicional exclusivo para owners
        if (isOwner) {
            ctx.append("\n\n").append(formatSkillMatrix(barbershopId));
            ctx.append("\n\n").append(formatServiceRevenueContext(barbershopId));
            ctx.append("\n\n").append(formatUncoveredServicesContext(barbershopId));
            ctx.append("\n\n").append(formatStockContext(barbershopId));
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

    private String formatCancelledContext(List<Appointment> list) {        StringBuilder sb = new StringBuilder("Agendamentos cancelados / não compareceu:\n");
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

    private boolean isOwnerUser(UserInfoDTO user, String headerRole) {
        if (user == null) return false;
        String role = ((user.getRole() != null ? user.getRole() : "") + " " + (headerRole != null ? headerRole : ""))
                .toUpperCase(Locale.ROOT);
        return role.contains("OWNER");
    }

    private String formatOperationalAppointmentMetrics(UUID barberId, UUID barbershopId, boolean isOwner, LocalDate today, LocalDateTime now) {
        try {
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDateTime todayStart = today.atStartOfDay();
            LocalDateTime todayEnd = today.plusDays(1).atStartOfDay().minusNanos(1);
            LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
            LocalDateTime tomorrowEnd = today.plusDays(2).atStartOfDay().minusNanos(1);
            LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
            LocalDateTime monthEnd = today.plusDays(1).atStartOfDay().minusNanos(1);

            List<Appointment> todayAppointments = findScopedAppointments(barberId, barbershopId, isOwner, todayStart, todayEnd);
            List<Appointment> tomorrowAppointments = findScopedAppointments(barberId, barbershopId, isOwner, tomorrowStart, tomorrowEnd);
            List<Appointment> monthAppointments = findScopedAppointments(barberId, barbershopId, isOwner, monthStartDateTime, monthEnd);

            long todayUpcoming = todayAppointments.stream()
                    .filter(a -> a.getStartTime() != null && !a.getStartTime().isBefore(now))
                    .filter(a -> !isLostStatus(a) && !isCompletedStatus(a))
                    .count();
            long todayCompleted = todayAppointments.stream().filter(this::isCompletedStatus).count();
            long tomorrowActive = tomorrowAppointments.stream().filter(a -> !isLostStatus(a) && !isCompletedStatus(a)).count();
            long monthCompleted = monthAppointments.stream().filter(this::isCompletedStatus).count();
            long monthLost = monthAppointments.stream().filter(this::isLostStatus).count();
            BigDecimal monthRevenue = monthAppointments.stream()
                    .filter(this::isCompletedStatus)
                    .map(a -> a.getTotalPrice() != null ? a.getTotalPrice() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            String scope = isOwner ? "toda a barbearia" : "apenas este barbeiro";
            StringBuilder sb = new StringBuilder("Agenda e atendimentos do painel (escopo: " + scope + "):\n");
            sb.append("- Hoje (").append(today.format(DATE_FMT)).append("): ")
                    .append(todayUpcoming).append(" próximos/ativos, ")
                    .append(todayCompleted).append(" concluídos\n");
            sb.append("- Amanhã (").append(today.plusDays(1).format(DATE_FMT)).append("): ")
                    .append(tomorrowActive).append(" agendamentos ativos\n");
            sb.append("- Mês atual (").append(monthStart.format(DATE_FMT)).append(" a ").append(today.format(DATE_FMT)).append("): ")
                    .append(monthCompleted).append(" atendimentos concluídos, ")
                    .append(monthLost).append(" cancelados/no-show, ")
                    .append("valor bruto em atendimentos concluídos: R$ ").append(formatMoney(monthRevenue)).append('\n');
            sb.append(formatTopServicesFromAppointments(monthAppointments));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular métricas operacionais — {}", e.getMessage());
            return "Agenda e atendimentos do painel: dados temporariamente indisponíveis.";
        }
    }

    private List<Appointment> findScopedAppointments(UUID barberId, UUID barbershopId, boolean isOwner, LocalDateTime from, LocalDateTime to) {
        if (isOwner && barbershopId != null) {
            return appointmentRepository.findByBarbershopIdAndStartTimeBetween(barbershopId, from, to);
        }
        return appointmentRepository.findByBarberIdAndStartTimeBetween(barberId, from, to);
    }

    private boolean isCompletedStatus(Appointment appointment) {
        if (appointment == null || appointment.getStatus() == null) return false;
        return switch (appointment.getStatus()) {
            case COMPLETED, CONCLUDED, WALK_IN -> true;
            default -> false;
        };
    }

    private boolean isLostStatus(Appointment appointment) {
        if (appointment == null || appointment.getStatus() == null) return false;
        return switch (appointment.getStatus()) {
            case CANCELLED, NO_SHOW -> true;
            default -> false;
        };
    }

    private String formatTopServicesFromAppointments(List<Appointment> appointments) {
        Map<String, ServiceStats> stats = new java.util.LinkedHashMap<>();
        appointments.stream()
                .filter(this::isCompletedStatus)
                .filter(a -> a.getActivities() != null)
                .flatMap(a -> a.getActivities().stream())
                .forEach(activity -> {
                    String name = activity.getActivityName() != null ? activity.getActivityName() : "Serviço sem nome";
                    ServiceStats current = stats.computeIfAbsent(name, key -> new ServiceStats());
                    current.count++;
                    current.revenue = current.revenue.add(activity.getPrice() != null ? activity.getPrice() : BigDecimal.ZERO);
                });

        if (stats.isEmpty()) return "- Serviços no mês atual: sem atendimentos concluídos.\n";

        var top = stats.entrySet().stream()
                .sorted((a, b) -> {
                    int countCompare = Long.compare(b.getValue().count, a.getValue().count);
                    if (countCompare != 0) return countCompare;
                    return b.getValue().revenue.compareTo(a.getValue().revenue);
                })
                .limit(5)
                .toList();

        StringBuilder sb = new StringBuilder("- Serviços mais executados no mês atual:\n");
        top.forEach(entry -> sb
                .append("  · ").append(entry.getKey())
                .append(": ").append(entry.getValue().count)
                .append(" vez(es), R$ ").append(formatMoney(entry.getValue().revenue))
                .append('\n'));
        return sb.toString();
    }

    private String formatDashboardFinancialContext(String firebaseUid, UUID barbershopId, UUID barberId, boolean isOwner, LocalDate today, LocalDateTime now) {
        LocalDate monthStart = today.withDayOfMonth(1);
        try {
            if (isOwner) {
                Map<String, Object> month = paymentServiceClient.getMyShopOverview(firebaseUid, barbershopId, monthStart, today);
                Map<String, Object> day = paymentServiceClient.getMyShopOverview(firebaseUid, barbershopId, today, today);
                if (month == null || day == null) {
                    return formatFinancialSummaryFromAppointments(barbershopId, now);
                }

                StringBuilder sb = new StringBuilder("Financeiro do painel do dono (mesma fonte do Dashboard):\n");
                sb.append("- Mês atual (").append(monthStart.format(DATE_FMT)).append(" a ").append(today.format(DATE_FMT)).append("):\n");
                appendOwnerOverview(sb, month, "  ");
                sb.append("- Hoje (").append(today.format(DATE_FMT)).append("): faturamento total R$ ")
                        .append(formatMoney(decimalValue(day, "totalServiceRevenue")))
                        .append(", resultado operacional total R$ ")
                        .append(formatMoney(decimalValue(day, "operationalResultWithWalkIn")))
                        .append(", atendimentos walk-in: ").append(intValue(day, "walkInAppointmentsCount"))
                        .append(", transações aprovadas: ").append(intValue(day, "approvedCount"))
                        .append('\n');

                List<Map<String, Object>> performance = paymentServiceClient.getMyShopBarberPerformance(firebaseUid, barbershopId, monthStart, today);
                if (performance != null && !performance.isEmpty()) {
                    sb.append("- Ranking de barbeiros no mês atual:\n");
                    performance.stream().limit(5).forEach(p -> sb
                            .append("  · ").append(firstNameOnly(String.valueOf(p.getOrDefault("barberName", "?"))))
                            .append(": receita gerada R$ ").append(formatMoney(decimalValue(p, "generatedRevenue")))
                            .append(", atendimentos: ").append(intValue(p, "totalAppointments"))
                            .append(", participação: ").append(formatMoney(decimalValue(p, "contributionPercentage"))).append("%")
                            .append('\n'));
                }
                return sb.toString();
            }

            if (barberId == null) {
                return "Financeiro do painel do barbeiro: dados não disponíveis para este usuário.";
            }

            Map<String, Object> month = paymentServiceClient.getMyBarberSummary(firebaseUid, barbershopId, monthStart, today);
            Map<String, Object> day = paymentServiceClient.getMyBarberSummary(firebaseUid, barbershopId, today, today);
            if (month == null || day == null) {
                List<Appointment> appointments = appointmentRepository.findCompletedByBarberId(barberId, monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1));
                return formatBarberCommissionContext(barbershopId, barberId, appointments);
            }

            StringBuilder sb = new StringBuilder("Financeiro do painel do barbeiro (mesma fonte do card de faturamento):\n");
            sb.append("- Mês atual (").append(monthStart.format(DATE_FMT)).append(" a ").append(today.format(DATE_FMT)).append("):\n");
            appendBarberSummary(sb, month, "  ");
            sb.append("- Hoje (").append(today.format(DATE_FMT)).append("): comissão total R$ ")
                    .append(formatMoney(decimalValue(day, "barberTotalCommission")))
                    .append(", valor bruto gerado R$ ")
                    .append(formatMoney(decimalValue(day, "grossTotalRevenue")))
                    .append(", comissão da barbearia R$ ")
                    .append(formatMoney(decimalValue(day, "barbershopTotalCommission")))
                    .append('\n');
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter financeiro do painel — {}", e.getMessage());
            if (isOwner && barbershopId != null) {
                return formatFinancialSummaryFromAppointments(barbershopId, now);
            }
            if (barberId != null && barbershopId != null) {
                List<Appointment> appointments = appointmentRepository.findCompletedByBarberId(barberId, monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1));
                return formatBarberCommissionContext(barbershopId, barberId, appointments);
            }
            return "Financeiro do painel: dados temporariamente indisponíveis.";
        }
    }

    private void appendOwnerOverview(StringBuilder sb, Map<String, Object> overview, String indent) {
        sb.append(indent).append("Faturamento total: R$ ").append(formatMoney(decimalValue(overview, "totalServiceRevenue"))).append('\n');
        sb.append(indent).append("Receita com transações: R$ ").append(formatMoney(decimalValue(overview, "serviceRevenue"))).append('\n');
        sb.append(indent).append("Receita walk-in: R$ ").append(formatMoney(decimalValue(overview, "walkInRevenue"))).append('\n');
        sb.append(indent).append("Gastos com produtos: R$ ").append(formatMoney(decimalValue(overview, "productExpenses"))).append('\n');
        sb.append(indent).append("Valor em estoque: R$ ").append(formatMoney(decimalValue(overview, "inventoryAssetValue"))).append('\n');
        sb.append(indent).append("Resultado operacional total: R$ ").append(formatMoney(decimalValue(overview, "operationalResultWithWalkIn"))).append('\n');
        sb.append(indent).append("Transações aprovadas: ").append(intValue(overview, "approvedCount"))
                .append(", pendentes: ").append(intValue(overview, "pendingCount"))
                .append(", canceladas: ").append(intValue(overview, "cancelledCount"))
                .append(", walk-ins: ").append(intValue(overview, "walkInAppointmentsCount"))
                .append('\n');
    }

    private void appendBarberSummary(StringBuilder sb, Map<String, Object> summary, String indent) {
        sb.append(indent).append("Comissão total do barbeiro: R$ ").append(formatMoney(decimalValue(summary, "barberTotalCommission"))).append('\n');
        sb.append(indent).append("Comissão com transações: R$ ").append(formatMoney(decimalValue(summary, "barberServiceCommission"))).append('\n');
        sb.append(indent).append("Comissão walk-in: R$ ").append(formatMoney(decimalValue(summary, "barberWalkInCommission"))).append('\n');
        sb.append(indent).append("Valor bruto gerado: R$ ").append(formatMoney(decimalValue(summary, "grossTotalRevenue"))).append('\n');
        sb.append(indent).append("Parte da barbearia: R$ ").append(formatMoney(decimalValue(summary, "barbershopTotalCommission"))).append('\n');
        sb.append(indent).append("Transações aprovadas: ").append(intValue(summary, "approvedCount"))
                .append(", pendentes: ").append(intValue(summary, "pendingCount"))
                .append(", canceladas: ").append(intValue(summary, "cancelledCount"))
                .append(", walk-ins: ").append(intValue(summary, "walkInAppointmentsCount"))
                .append('\n');
    }

    private BigDecimal decimalValue(Map<String, Object> data, String key) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return BigDecimal.ZERO;
        Object value = data.get(key);
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private int intValue(Map<String, Object> data, String key) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return 0;
        Object value = data.get(key);
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safe = value != null ? value : BigDecimal.ZERO;
        return safe.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static class ServiceStats {
        long count;
        BigDecimal revenue = BigDecimal.ZERO;
    }

    private String formatSkillMatrix(UUID barbershopId) {
        try {
            var rows = vBarberSkillMatrixRepository.findByBarbershopId(barbershopId.toString());
            if (rows.isEmpty()) return "Habilidades dos barbeiros: sem dados disponíveis.";
            StringBuilder sb = new StringBuilder("Habilidades e serviços executados por barbeiro (últimos 90 dias):\n");
            rows.forEach(r -> sb
                    .append("- ")
                    .append(firstNameOnly(r.getBarberName()))
                    .append(" | serviço: ").append(r.getActivityName())
                    .append(" | vezes executado: ").append(r.getTimesExecuted())
                    .append(" | receita gerada: R$ ").append(
                            r.getTotalGeneratedByActivity() != null
                                    ? r.getTotalGeneratedByActivity().toPlainString()
                                    : "0.00")
                    .append('\n'));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter skill matrix — {}", e.getMessage());
            return "Habilidades dos barbeiros: dados temporariamente indisponíveis.";
        }
    }

    /**
     * Agrega receita por serviço (globalmente na barbearia), somando totalGeneratedByActivity
     * de todos os barbeiros por nome de serviço.
     * Responde: "Qual serviço gerou mais receita?" e "Qual o serviço mais executado?"
     */
    private String formatServiceRevenueContext(UUID barbershopId) {
        try {
            var rows = vBarberSkillMatrixRepository.findByBarbershopId(barbershopId.toString());
            if (rows.isEmpty()) return "Receita por serviço: sem dados disponíveis.";

            // Agrega por nome de serviço
            Map<String, long[]> byService = new java.util.LinkedHashMap<>();
            rows.forEach(r -> {
                String svc = r.getActivityName() != null ? r.getActivityName() : "Desconhecido";
                long times = r.getTimesExecuted() != null ? r.getTimesExecuted() : 0;
                long cents = r.getTotalGeneratedByActivity() != null
                        ? r.getTotalGeneratedByActivity().multiply(java.math.BigDecimal.valueOf(100)).longValue()
                        : 0;
                byService.merge(svc, new long[]{times, cents}, (a, b) -> new long[]{a[0] + b[0], a[1] + b[1]});
            });

            // Ordena por receita desc
            var sorted = byService.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]))
                    .toList();

            StringBuilder sb = new StringBuilder("Receita e execuções por serviço (90 dias, barbearia toda):\n");
            sorted.forEach(e -> sb
                    .append("- ").append(e.getKey())
                    .append(" | vezes: ").append(e.getValue()[0])
                    .append(" | receita total: R$ ")
                    .append(java.math.BigDecimal.valueOf(e.getValue()[1])
                            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).toPlainString())
                    .append('\n'));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular receita por serviço — {}", e.getMessage());
            return "Receita por serviço: dados temporariamente indisponíveis.";
        }
    }

    /**
     * Cruza o catálogo de atividades da barbearia com a skill matrix para identificar
     * quais barbeiros NÃO executaram determinado serviço nos últimos 90 dias.
     * Responde: "Algum barbeiro nunca executou algum serviço?"
     */
    private String formatUncoveredServicesContext(UUID barbershopId) {
        try {
            List<ActivityInfoDTO> allActivities = barbershopServiceClient.getAllActivities(barbershopId);
            if (allActivities == null || allActivities.isEmpty()) {
                return "Cobertura de serviços: nenhum serviço cadastrado na barbearia.";
            }

            var rows = vBarberSkillMatrixRepository.findByBarbershopId(barbershopId.toString());
            if (rows.isEmpty()) {
                return "Cobertura de serviços: nenhum dado de execução disponível nos últimos 90 dias.";
            }

            // Conjunto de pares "barbeiro:serviço" que foram executados
            java.util.Set<String> executed = new java.util.HashSet<>();
            java.util.Set<String> barberNames = new java.util.LinkedHashSet<>();
            rows.forEach(r -> {
                executed.add(firstNameOnly(r.getBarberName()) + ":" + r.getActivityName());
                barberNames.add(firstNameOnly(r.getBarberName()));
            });

            // Para cada barbeiro, lista serviços NÃO executados
            StringBuilder sb = new StringBuilder("Serviços não executados por barbeiro (90 dias):\n");
            boolean algumaSemCobertura = false;
            for (String barberName : barberNames) {
                List<String> naoExecutados = allActivities.stream()
                        .map(ActivityInfoDTO::getActivityName)
                        .filter(svc -> !executed.contains(barberName + ":" + svc))
                        .toList();
                if (!naoExecutados.isEmpty()) {
                    sb.append("- ").append(barberName).append(" não executou: ")
                            .append(String.join(", ", naoExecutados)).append('\n');
                    algumaSemCobertura = true;
                }
            }

            if (!algumaSemCobertura) {
                sb.append("Todos os barbeiros executaram todos os serviços cadastrados no período.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular cobertura de serviços — {}", e.getMessage());
            return "Cobertura de serviços: dados temporariamente indisponíveis.";
        }
    }

    private String formatStockContext(UUID barbershopId) {
        try {
            List<Map<String, Object>> items = productServiceClient.getStockHealth(barbershopId);
            if (items == null || items.isEmpty()) return "Estoque: sem produtos cadastrados ou sem alertas no momento.";

            StringBuilder sb = new StringBuilder("Situação do estoque:\n");
            List<Map<String, Object>> zerados = items.stream()
                    .filter(i -> {
                        Object qty = i.get("currentStock");
                        return qty != null && ((Number) qty).intValue() == 0;
                    }).toList();
            List<Map<String, Object>> criticos = items.stream()
                    .filter(i -> Boolean.TRUE.equals(i.get("requiresRestock"))
                            && (i.get("currentStock") == null || ((Number) i.get("currentStock")).intValue() > 0))
                    .toList();
            List<Map<String, Object>> ok = items.stream()
                    .filter(i -> !Boolean.TRUE.equals(i.get("requiresRestock"))).toList();

            if (!zerados.isEmpty()) {
                sb.append("  [ZERADO — estoque 0]:\n");
                zerados.forEach(item -> sb
                        .append("    - ").append(item.getOrDefault("productName", "?"))
                        .append(" | categoria: ").append(item.getOrDefault("category", "?"))
                        .append(" | mínimo: ").append(item.getOrDefault("predictedMinimum", "?"))
                        .append('\n'));
            }
            if (!criticos.isEmpty()) {
                sb.append("  [CRÍTICO — abaixo do mínimo]:\n");
                criticos.forEach(item -> sb
                        .append("    - ").append(item.getOrDefault("productName", "?"))
                        .append(" | categoria: ").append(item.getOrDefault("category", "?"))
                        .append(" | qtd atual: ").append(item.getOrDefault("currentStock", "?"))
                        .append(" | mínimo: ").append(item.getOrDefault("predictedMinimum", "?"))
                        .append('\n'));
            }
            if (!ok.isEmpty()) {
                sb.append("  [OK — estoque suficiente]:\n");
                ok.forEach(item -> sb
                        .append("    - ").append(item.getOrDefault("productName", "?"))
                        .append(" | qtd: ").append(item.getOrDefault("currentStock", "?"))
                        .append('\n'));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter estoque — {}", e.getMessage());
            return "Estoque: dados temporariamente indisponíveis.";
        }
    }

    /**
     * Para barbeiro colaborador: calcula receita bruta e comissão líquida
     * cruzando os appointments concluídos com as regras de comissão do barbershop-service.
     */
    private String formatBarberCommissionContext(UUID barbershopId, UUID barberId, List<Appointment> appointments) {
        try {
            List<CommissionRuleInfoDTO> rules = barbershopServiceClient.getBarberCommissions(barbershopId, barberId);

            java.math.BigDecimal totalBruto = java.math.BigDecimal.ZERO;
            java.math.BigDecimal totalComissao = java.math.BigDecimal.ZERO;

            // índice rápido activityId → percentual
            Map<UUID, java.math.BigDecimal> ruleMap = new java.util.HashMap<>();
            rules.forEach(r -> ruleMap.put(r.activityId(), r.percentage()));

            for (Appointment a : appointments) {
                if (a.getActivities() == null) continue;
                for (var act : a.getActivities()) {
                    java.math.BigDecimal preco = act.getPrice() != null ? act.getPrice() : java.math.BigDecimal.ZERO;
                    totalBruto = totalBruto.add(preco);
                    java.math.BigDecimal pct = ruleMap.get(act.getActivityId());
                    if (pct != null) {
                        totalComissao = totalComissao.add(
                                preco.multiply(pct).divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                        );
                    }
                }
            }

            StringBuilder sb = new StringBuilder("Resumo financeiro pessoal (últimos 90 dias):\n");
            sb.append("- Receita bruta gerada: R$ ").append(totalBruto.toPlainString()).append('\n');
            if (!rules.isEmpty()) {
                sb.append("- Comissão líquida (conforme regras do dono): R$ ").append(totalComissao.toPlainString()).append('\n');
                sb.append("- Regras de comissão cadastradas:\n");
                rules.forEach(r -> sb.append("  · ").append(r.activityName())
                        .append(": ").append(r.percentage().toPlainString()).append("%\n"));
            } else {
                sb.append("- Comissão: nenhuma regra cadastrada pelo dono ainda.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular comissão do colaborador — {}", e.getMessage());
            return "Resumo financeiro pessoal: dados temporariamente indisponíveis.";
        }
    }

    /**
     * Calcula resumo financeiro diretamente dos appointments concluídos no schedule_db.
     * Não depende do payment-service — fonte primária enquanto transactions estiver vazia.
     */
    private String formatFinancialSummaryFromAppointments(UUID barbershopId, LocalDateTime now) {
        try {
            LocalDateTime from30 = now.minusDays(30);
            LocalDateTime from90 = now.minusDays(90);

            List<Appointment> ultimos30 = appointmentRepository.findCompletedByBarbershop(barbershopId, from30, now);
            List<Appointment> ultimos90 = appointmentRepository.findCompletedByBarbershop(barbershopId, from90, now);

            if (ultimos90.isEmpty()) return "Receita calculada: nenhum atendimento concluído nos últimos 90 dias.";

            java.math.BigDecimal total30 = ultimos30.stream()
                    .filter(a -> a.getTotalPrice() != null)
                    .map(Appointment::getTotalPrice)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal total90 = ultimos90.stream()
                    .filter(a -> a.getTotalPrice() != null)
                    .map(Appointment::getTotalPrice)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal mediaDiaria = total30.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? total30.divide(java.math.BigDecimal.valueOf(30), 2, java.math.RoundingMode.HALF_UP)
                    : java.math.BigDecimal.ZERO;
            java.math.BigDecimal previsaoMes = mediaDiaria.multiply(java.math.BigDecimal.valueOf(30));

            Map<String, java.math.BigDecimal> porBarbeiro = new java.util.LinkedHashMap<>();
            ultimos90.forEach(a -> {
                String nome = firstNameOnly(a.getBarberName());
                porBarbeiro.merge(nome,
                        a.getTotalPrice() != null ? a.getTotalPrice() : java.math.BigDecimal.ZERO,
                        java.math.BigDecimal::add);
            });

            StringBuilder sb = new StringBuilder("Resumo financeiro (baseado em atendimentos concluídos):\n");
            sb.append("- Receita últimos 30 dias: R$ ").append(total30.toPlainString()).append('\n');
            sb.append("- Receita últimos 90 dias: R$ ").append(total90.toPlainString()).append('\n');
            sb.append("- Atendimentos concluídos últimos 30 dias: ").append(ultimos30.size()).append('\n');
            sb.append("- Atendimentos concluídos últimos 90 dias: ").append(ultimos90.size()).append('\n');

            // Ticket médio explícito — resolve a lacuna de inferência do modelo
            if (!ultimos30.isEmpty() && total30.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal ticketMedio30 = total30.divide(
                        java.math.BigDecimal.valueOf(ultimos30.size()), 2, java.math.RoundingMode.HALF_UP);
                sb.append("- Ticket médio por atendimento (30 dias): R$ ").append(ticketMedio30.toPlainString()).append('\n');
            }
            if (!ultimos90.isEmpty() && total90.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal ticketMedio90 = total90.divide(
                        java.math.BigDecimal.valueOf(ultimos90.size()), 2, java.math.RoundingMode.HALF_UP);
                sb.append("- Ticket médio por atendimento (90 dias): R$ ").append(ticketMedio90.toPlainString()).append('\n');
            }

            sb.append("- Projeção mensal (média × 30 dias): R$ ").append(previsaoMes.toPlainString()).append('\n');
            if (!porBarbeiro.isEmpty()) {
                sb.append("- Receita por barbeiro (90 dias):\n");
                porBarbeiro.forEach((nome, valor) ->
                        sb.append("  · ").append(nome).append(": R$ ").append(valor.toPlainString()).append('\n'));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular resumo financeiro local — {}", e.getMessage());
            return "Resumo financeiro: dados temporariamente indisponíveis.";
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

    private String buildPrompt(String context, String history, String message, UserInfoDTO user, String userRole) {
        String nomeUsuario = user != null && user.getName() != null
                ? user.getName().trim().split("\\s+")[0]
                : "usuário";
        boolean isCustomer = user != null && "CUSTOMER".equalsIgnoreCase(user.getUserType());
        boolean isOwner    = isOwnerUser(user, userRole);
        String perfil = isOwner ? "DONO DE BARBEARIA" : isCustomer ? "CLIENTE" : "BARBEIRO COLABORADOR";
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
                8. Quando o usuário perguntar sobre "mês", "este mês", "rendimento", "faturamento", "ganhei" ou "recebi", use o bloco "Mês atual" do financeiro do painel. Não troque por "últimos 30 dias" nem por projeção.
                9. Para DONO, "rendimento/faturamento do mês" significa faturamento total da barbearia; se ele pedir lucro/resultado, use resultado operacional total. Para BARBEIRO COLABORADOR, "rendimento/quanto recebi" significa comissão total do barbeiro.
                10. Se existirem várias métricas parecidas, escolha a mais aderente à pergunta e cite o rótulo exato usado. Não diga que um único valor representa 30 dias, 90 dias e projeção ao mesmo tempo.
                %s

                Pergunta: %s
                """.formatted(
                nomeUsuario,
                perfil,
                isOwner
                        ? "- Acesso: agenda completa da barbearia, financeiro, estoque e equipe"
                        : "- Acesso: apenas seus próprios agendamentos e financeiro pessoal",
                context,
                historyBlock,
                isOwner
                        ? ""
                        : "11. Este usuário é colaborador, não dono. Não forneça dados financeiros globais da barbearia, apenas os dados dele.",
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
