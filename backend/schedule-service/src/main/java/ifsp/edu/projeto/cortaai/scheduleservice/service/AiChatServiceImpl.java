package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.service.ChatHistoryService;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CommissionRuleInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.WorkBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserAnalyticsClient;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
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
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_APPOINTMENTS_CONTEXT = 50;

    private final AppointmentRepository appointmentRepository;
    private final VBarberSkillMatrixRepository vBarberSkillMatrixRepository;
    private final UserServiceClient userServiceClient;
    private final UserAnalyticsClient userAnalyticsClient;
    private final BarbershopServiceClient barbershopServiceClient;
    private final ProductServiceClient productServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final ChatHistoryService chatHistoryService;
    private final RestTemplate restTemplate;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.url}")
    private String geminiUrl;

    @Value("${ai.groq.api-key:}")
    private String groqApiKey;

    @Value("${ai.groq.url}")
    private String groqUrl;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${ai.openrouter.api-key:}")
    private String openrouterApiKey;

    @Value("${ai.openrouter.url}")
    private String openrouterUrl;

    @Value("${ai.openrouter.model}")
    private String openrouterModel;

    @Value("${ai.cohere.api-key:}")
    private String cohereApiKey;

    @Value("${ai.cohere.url}")
    private String cohereUrl;

    @Value("${ai.cohere.model:command-r}")
    private String cohereModel;

    @Value("${app.timezone:America/Sao_Paulo}")
    private String appTimezone = "America/Sao_Paulo";

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
        LocalDateTime now  = getNowInAppTimezone();
        LocalDate today = now.toLocalDate();

        StringBuilder ctx = new StringBuilder();
        ctx.append("Data e hora de referência do sistema para o usuário: ")
                .append(now.format(FMT))
                .append(" (fuso ")
                .append(resolveZoneId().getId())
                .append("). Hoje é ")
                .append(today.format(DATE_FMT))
                .append("; amanhã é ")
                .append(today.plusDays(1).format(DATE_FMT))
                .append(".\n\n");

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
            ctx.append("\n\n").append(formatWorkScheduleAndAvailability(internalId, barbershopId, isOwner, today, now));
            ctx.append("\n\n").append(formatCancellationByWeekdayMetrics(internalId, barbershopId, isOwner, today, now));
            ctx.append("\n\n").append(formatCustomerFrequencyContext(internalId, barbershopId, isOwner, now));
        }

        if (!isCustomer && barbershopId != null) {
            ctx.append("\n\n").append(formatDashboardFinancialContext(firebaseUid, barbershopId, internalId, isOwner, today, now));
        }

        // 2–4. Contexto adicional exclusivo para owners
        if (isOwner) {
            ctx.append("\n\n").append(formatSkillMatrix(barbershopId));
            ctx.append("\n\n").append(formatServiceRevenueContext(barbershopId));
            ctx.append("\n\n").append(formatServicePromotionContext(barbershopId, now));
            ctx.append("\n\n").append(formatUncoveredServicesContext(barbershopId));
            ctx.append("\n\n").append(formatTeamContext(barbershopId, internalId));
            ctx.append("\n\n").append(formatCustomerAcquisitionContext(today));
            ctx.append("\n\n").append(formatStockContext(barbershopId));
        }

        return ctx.toString();
    }

    private LocalDateTime getNowInAppTimezone() {
        return LocalDateTime.now(resolveZoneId());
    }

    private ZoneId resolveZoneId() {
        try {
            String timezone = appTimezone == null || appTimezone.isBlank()
                    ? "America/Sao_Paulo"
                    : appTimezone;
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("gustavo: timezone inválido em app.timezone='{}'; usando America/Sao_Paulo", appTimezone);
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    private String translateStatus(Object status) {
        if (status == null) return "agendado";
        return switch (status.toString()) {
            case "SCHEDULED"       -> "agendado";
            case "CONFIRMED"       -> "confirmado";
            case "IN_PROGRESS"     -> "em atendimento";
            case "COMPLETED",
                 "CONCLUDED"       -> "concluído";
            case "WALK_IN"         -> "encaixe (walk-in)";
            case "CANCELLED"       -> "cancelado";
            case "NO_SHOW"         -> "não compareceu";
            case "PAYMENT_PENDING" -> "aguardando pagamento";
            case "EXPIRED"         -> "expirado";
            default                -> status.toString().toLowerCase();
        };
    }

    private String formatPreviewContext(List<Appointment> list) {
        if (list.isEmpty()) return "Nenhum atendimento futuro na agenda.";
        StringBuilder sb = new StringBuilder("Próximos atendimentos agendados (até " + MAX_APPOINTMENTS_CONTEXT + " itens):\n");
        list.stream().limit(MAX_APPOINTMENTS_CONTEXT).forEach(a -> sb
                .append("- ")
                .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
                .append(" | cliente: ")
                .append(firstNameOnly(a.getCustomerName()))
                .append(" | barbeiro: ")
                .append(firstNameOnly(a.getBarberName()))
                .append(" | situação: ")
                .append(translateStatus(a.getStatus()))
                .append('\n'));
        return sb.toString();
    }

    private String formatPreviewContextCustomer(List<Appointment> list) {
        if (list.isEmpty()) return "Você não possui atendimentos futuros agendados.";
        StringBuilder sb = new StringBuilder("Seus próximos atendimentos agendados:\n");
        list.stream().limit(MAX_APPOINTMENTS_CONTEXT).forEach(a -> {
            String servicos = a.getActivities() == null || a.getActivities().isEmpty() ? "—"
                    : a.getActivities().stream().map(act -> act.getActivityName()).collect(java.util.stream.Collectors.joining(", "));
            sb.append("- ")
              .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
              .append(" | barbeiro: ").append(firstNameOnly(a.getBarberName()))
              .append(" | serviço: ").append(servicos)
              .append(" | situação: ").append(translateStatus(a.getStatus()))
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
                .append(" | cliente: ")
                .append(firstNameOnly(a.getCustomerName()))
                .append(" | barbeiro: ")
                .append(firstNameOnly(a.getBarberName()))
                .append(" | valor: R$ ")
                .append(a.getTotalPrice() != null ? formatMoney(a.getTotalPrice()) : "0,00")
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
        StringBuilder sb = new StringBuilder("Atendimentos cancelados ou com falta do cliente:\n");
        list.stream().limit(20).forEach(a -> sb
                .append("- ")
                .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
                .append(" | ").append(firstNameOnly(a.getBarberName()))
                .append(" | motivo: ").append(translateStatus(a.getStatus()))
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

            List<Appointment> todayAppointments = safeList(findScopedAppointments(barberId, barbershopId, isOwner, todayStart, todayEnd));
            List<Appointment> tomorrowAppointments = safeList(findScopedAppointments(barberId, barbershopId, isOwner, tomorrowStart, tomorrowEnd));
            List<Appointment> monthAppointments = safeList(findScopedAppointments(barberId, barbershopId, isOwner, monthStartDateTime, monthEnd));
            List<Appointment> upcomingAppointments = safeList(findScopedAppointments(barberId, barbershopId, isOwner, now, now.plusDays(14)));

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
            StringBuilder sb = new StringBuilder("Resumo da agenda e atendimentos (" + scope + "):\n");
            sb.append("- Hoje (").append(today.format(DATE_FMT)).append("): ")
                    .append(todayUpcoming).append(" atendimento(s) ainda por vir, ")
                    .append(todayCompleted).append(" já concluído(s)\n");
            sb.append("- Amanhã (").append(today.plusDays(1).format(DATE_FMT)).append("): ")
                    .append(tomorrowActive).append(" atendimento(s) agendado(s)\n");
            sb.append("- Mês atual (").append(monthStart.format(DATE_FMT)).append(" a ").append(today.format(DATE_FMT)).append("): ")
                    .append(monthCompleted).append(" atendimento(s) concluído(s), ")
                    .append(monthLost).append(" cancelado(s)/falta do cliente, ")
                    .append("receita bruta dos concluídos: R$ ").append(formatMoney(monthRevenue)).append('\n');
            sb.append(formatUpcomingAppointments(upcomingAppointments));
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

    private String formatUpcomingAppointments(List<Appointment> appointments) {
        List<Appointment> active = safeList(appointments).stream()
                .filter(a -> !isLostStatus(a) && !isCompletedStatus(a))
                .sorted(java.util.Comparator.comparing(Appointment::getStartTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .limit(10)
                .toList();
        if (active.isEmpty()) {
            return "- Próximos 14 dias: nenhum atendimento ativo encontrado.\n";
        }
        StringBuilder sb = new StringBuilder("- Próximos 14 dias:\n");
        active.forEach(a -> sb
                .append("  · ")
                .append(a.getStartTime() != null ? a.getStartTime().format(FMT) : "?")
                .append(" | cliente: ").append(firstNameOnly(a.getCustomerName()))
                .append(" | barbeiro: ").append(firstNameOnly(a.getBarberName()))
                .append(" | situação: ").append(translateStatus(a.getStatus()))
                .append('\n'));
        return sb.toString();
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
        safeList(appointments).stream()
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

    private int intValueAny(Object value) {
        if (value == null) return 0;
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

    private BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : List.of();
    }

    private String displayDay(DayOfWeek day) {
        if (day == null) return "dia não informado";
        return day.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
    }

    private String formatBlocks(List<WorkBlockDTO> blocks) {
        return safeList(blocks).stream()
                .map(b -> formatTime(b.getStartTime()) + "-" + formatTime(b.getEndTime()))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String formatTime(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : "?";
    }

    private List<WorkBlockDTO> blocksForDay(List<DayScheduleDTO> schedule, DayOfWeek day) {
        return safeList(schedule).stream()
                .filter(d -> day.equals(d.getDayOfWeek()))
                .findFirst()
                .map(DayScheduleDTO::getBlocks)
                .map(this::safeList)
                .orElse(List.of());
    }

    private String formatOccupiedWindows(List<Appointment> appointments) {
        List<Appointment> active = safeList(appointments).stream()
                .filter(a -> !isLostStatus(a) && !isCompletedStatus(a))
                .sorted(java.util.Comparator.comparing(Appointment::getStartTime, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .toList();
        if (active.isEmpty()) return "sem atendimentos ativos";
        return active.stream()
                .map(a -> formatTime(a.getStartTime() != null ? a.getStartTime().toLocalTime() : null)
                        + "-" + formatTime(a.getEndTime() != null ? a.getEndTime().toLocalTime() : null)
                        + " com " + firstNameOnly(a.getCustomerName()))
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private String formatLunchAvailability(List<WorkBlockDTO> blocks, List<Appointment> appointments, LocalDate date) {
        if (safeList(blocks).isEmpty()) return "sem expediente cadastrado para hoje";
        LocalTime lunchStart = LocalTime.of(11, 0);
        LocalTime lunchEnd = LocalTime.of(14, 0);
        boolean hasWindow = safeList(blocks).stream()
                .anyMatch(b -> intervalsOverlap(b.getStartTime(), b.getEndTime(), lunchStart, lunchEnd));
        if (!hasWindow) return "fora do expediente cadastrado";

        LocalDateTime check = date.atTime(LocalTime.NOON);
        boolean noonBusy = safeList(appointments).stream()
                .filter(a -> !isLostStatus(a) && !isCompletedStatus(a))
                .anyMatch(a -> containsInstant(a.getStartTime(), a.getEndTime(), check));
        if (!noonBusy && isInsideAnyBlock(blocks, LocalTime.NOON)) {
            return "12:00 está livre; há espaço para almoço nesse intervalo";
        }
        return "há expediente nesse intervalo, mas 12:00 está ocupado ou fora de bloco; confira os horários ocupados acima";
    }

    private String formatExactAvailability(List<WorkBlockDTO> blocks, List<Appointment> appointments, LocalDate date, LocalTime time) {
        if (safeList(blocks).isEmpty()) return "sem expediente cadastrado para esse dia";
        if (!isInsideAnyBlock(blocks, time)) return "fora do expediente cadastrado";
        LocalDateTime dateTime = date.atTime(time);
        boolean busy = safeList(appointments).stream()
                .filter(a -> !isLostStatus(a) && !isCompletedStatus(a))
                .anyMatch(a -> containsInstant(a.getStartTime(), a.getEndTime(), dateTime));
        return busy ? "ocupado por atendimento já marcado" : "livre dentro do expediente";
    }

    private boolean isInsideAnyBlock(List<WorkBlockDTO> blocks, LocalTime time) {
        return safeList(blocks).stream()
                .anyMatch(b -> b.getStartTime() != null && b.getEndTime() != null
                        && !time.isBefore(b.getStartTime())
                        && time.isBefore(b.getEndTime()));
    }

    private boolean intervalsOverlap(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        if (startA == null || endA == null || startB == null || endB == null) return false;
        return startA.isBefore(endB) && endA.isAfter(startB);
    }

    private boolean containsInstant(LocalDateTime start, LocalDateTime end, LocalDateTime instant) {
        if (start == null || end == null || instant == null) return false;
        return !instant.isBefore(start) && instant.isBefore(end);
    }

    private BigDecimal averageIntervalDays(List<LocalDate> dates) {
        if (dates == null || dates.size() < 2) return null;
        dates.sort(java.util.Comparator.naturalOrder());
        long total = 0;
        int intervals = 0;
        for (int i = 1; i < dates.size(); i++) {
            total += ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
            intervals++;
        }
        if (intervals == 0) return null;
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(intervals), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageServiceRecurrence(Map<UUID, List<LocalDate>> customerDates) {
        long totalDays = 0;
        int intervals = 0;
        for (List<LocalDate> dates : customerDates.values()) {
            if (dates == null || dates.size() < 2) continue;
            dates.sort(java.util.Comparator.naturalOrder());
            for (int i = 1; i < dates.size(); i++) {
                totalDays += ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
                intervals++;
            }
        }
        if (intervals == 0) return null;
        return BigDecimal.valueOf(totalDays).divide(BigDecimal.valueOf(intervals), 2, RoundingMode.HALF_UP);
    }

    private long acquisitionValue(List<Map<String, Object>> rows, String month) {
        return safeList(rows).stream()
                .filter(row -> month.equals(String.valueOf(row.get("referenceMonth"))))
                .findFirst()
                .map(row -> {
                    Object value = row.get("newCustomers");
                    if (value instanceof Number number) return number.longValue();
                    try {
                        return Long.parseLong(String.valueOf(value));
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                })
                .orElse(0L);
    }

    private static class ServiceStats {
        long count;
        BigDecimal revenue = BigDecimal.ZERO;
    }

    private static class WeekdayStats {
        long total;
        long lost;
    }

    private static class CustomerStats {
        long completed;
        List<LocalDate> dates = new java.util.ArrayList<>();
    }

    private static class ServicePromotionStats {
        long count;
        BigDecimal revenue = BigDecimal.ZERO;
        Map<UUID, List<LocalDate>> customerDates = new java.util.LinkedHashMap<>();
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
            int totalProducts = items.size();
            int totalUnits = items.stream().mapToInt(i -> intValueAny(i.get("currentStock"))).sum();
            long aboveMinimum = items.stream()
                    .filter(i -> intValueAny(i.get("currentStock")) > intValueAny(i.get("predictedMinimum")))
                    .count();
            long veryHigh = items.stream()
                    .filter(i -> {
                        int minimum = intValueAny(i.get("predictedMinimum"));
                        int current = intValueAny(i.get("currentStock"));
                        return minimum > 0 && current >= minimum * 3;
                    })
                    .count();
            sb.append("- Resumo: ").append(totalProducts).append(" produto(s), ")
                    .append(totalUnits).append(" unidade(s) em estoque, ")
                    .append(aboveMinimum).append(" acima do mínimo e ")
                    .append(veryHigh).append(" muito acima do mínimo (3x ou mais).\n");
            if (veryHigh == 0) {
                sb.append("- Leitura sobre estoque alto: não há sinal de estoque alto pelos mínimos cadastrados.\n");
            } else {
                sb.append("- Leitura sobre estoque alto: há ").append(veryHigh)
                        .append(" produto(s) muito acima do mínimo; vale revisar giro antes de comprar mais.\n");
            }
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

    private String formatWorkScheduleAndAvailability(UUID barberId, UUID barbershopId, boolean isOwner, LocalDate today, LocalDateTime now) {
        try {
            List<DayScheduleDTO> schedule = safeList(userServiceClient.getBarberWorkSchedule(barberId));
            List<Appointment> todayAppointments = safeList(findScopedAppointments(
                    barberId, barbershopId, false, today.atStartOfDay(), today.plusDays(1).atStartOfDay().minusNanos(1)));
            List<Appointment> tomorrowAppointments = safeList(findScopedAppointments(
                    barberId, barbershopId, false, today.plusDays(1).atStartOfDay(), today.plusDays(2).atStartOfDay().minusNanos(1)));

            StringBuilder sb = new StringBuilder("Agenda de trabalho e disponibilidade do usuário logado:\n");
            if (schedule.isEmpty()) {
                sb.append("- Dias de trabalho cadastrados: não encontrei blocos de horário.\n");
            } else {
                sb.append("- Dias de trabalho cadastrados:\n");
                schedule.stream()
                        .filter(d -> d.getBlocks() != null && !d.getBlocks().isEmpty())
                        .forEach(d -> sb.append("  · ")
                                .append(displayDay(d.getDayOfWeek()))
                                .append(": ")
                                .append(formatBlocks(d.getBlocks()))
                                .append('\n'));
            }

            sb.append("- Ocupação de hoje (").append(today.format(DATE_FMT)).append("): ")
                    .append(formatOccupiedWindows(todayAppointments))
                    .append('\n');
            sb.append("- Ocupação de amanhã (").append(today.plusDays(1).format(DATE_FMT)).append("): ")
                    .append(formatOccupiedWindows(tomorrowAppointments))
                    .append('\n');

            var todayBlocks = blocksForDay(schedule, today.getDayOfWeek());
            var tomorrowBlocks = blocksForDay(schedule, today.plusDays(1).getDayOfWeek());
            sb.append("- Janela de almoço hoje (11:00-14:00): ")
                    .append(formatLunchAvailability(todayBlocks, todayAppointments, today))
                    .append('\n');
            sb.append("- Amanhã às 12:00: ")
                    .append(formatExactAvailability(tomorrowBlocks, tomorrowAppointments, today.plusDays(1), LocalTime.NOON))
                    .append('\n');
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter agenda de trabalho — {}", e.getMessage());
            return "Agenda de trabalho: dados temporariamente indisponíveis.";
        }
    }

    private String formatCancellationByWeekdayMetrics(UUID barberId, UUID barbershopId, boolean isOwner, LocalDate today, LocalDateTime now) {
        try {
            List<Appointment> appointments = safeList(findScopedAppointments(
                    barberId, barbershopId, isOwner, today.minusDays(90).atStartOfDay(), now));
            if (appointments.isEmpty()) {
                return "Cancelamentos por dia da semana (últimos 90 dias): sem agendamentos no período.";
            }

            Map<DayOfWeek, WeekdayStats> stats = new java.util.EnumMap<>(DayOfWeek.class);
            for (Appointment appointment : appointments) {
                if (appointment.getStartTime() == null) continue;
                DayOfWeek day = appointment.getStartTime().getDayOfWeek();
                WeekdayStats row = stats.computeIfAbsent(day, ignored -> new WeekdayStats());
                row.total++;
                if (isLostStatus(appointment)) row.lost++;
            }

            var top = stats.entrySet().stream()
                    .filter(e -> e.getValue().lost > 0)
                    .sorted((a, b) -> {
                        int rateCompare = percent(b.getValue().lost, b.getValue().total)
                                .compareTo(percent(a.getValue().lost, a.getValue().total));
                        if (rateCompare != 0) return rateCompare;
                        return Long.compare(b.getValue().lost, a.getValue().lost);
                    })
                    .findFirst();

            StringBuilder sb = new StringBuilder("Cancelamentos por dia da semana (últimos 90 dias):\n");
            if (top.isPresent()) {
                WeekdayStats row = top.get().getValue();
                sb.append("- Maior taxa: ").append(displayDay(top.get().getKey()))
                        .append(" com ").append(formatMoney(percent(row.lost, row.total)))
                        .append("% (").append(row.lost).append(" de ").append(row.total).append(" agendamentos).\n");
            } else {
                sb.append("- Não houve cancelamentos ou faltas no período.\n");
            }
            stats.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sb.append("  · ")
                            .append(displayDay(e.getKey())).append(": ")
                            .append(e.getValue().lost).append(" cancelamento(s)/falta em ")
                            .append(e.getValue().total).append(" agendamento(s), taxa ")
                            .append(formatMoney(percent(e.getValue().lost, e.getValue().total))).append("%\n"));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular cancelamentos por dia — {}", e.getMessage());
            return "Cancelamentos por dia da semana: dados temporariamente indisponíveis.";
        }
    }

    private String formatCustomerFrequencyContext(UUID barberId, UUID barbershopId, boolean isOwner, LocalDateTime now) {
        try {
            List<Appointment> appointments = safeList(findScopedAppointments(barberId, barbershopId, isOwner, now.minusDays(180), now));
            Map<String, CustomerStats> stats = new java.util.LinkedHashMap<>();
            appointments.stream()
                    .filter(this::isCompletedStatus)
                    .filter(a -> a.getCustomerName() != null && !a.getCustomerName().isBlank())
                    .forEach(a -> {
                        String customerName = firstNameOnly(a.getCustomerName());
                        CustomerStats row = stats.computeIfAbsent(customerName, ignored -> new CustomerStats());
                        row.completed++;
                        if (a.getStartTime() != null) row.dates.add(a.getStartTime().toLocalDate());
                    });
            if (stats.isEmpty()) {
                return "Frequência de clientes (últimos 180 dias): sem clientes com atendimentos concluídos.";
            }

            StringBuilder sb = new StringBuilder("Frequência de clientes (últimos 180 dias):\n");
            stats.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue().completed, a.getValue().completed))
                    .limit(20)
                    .forEach(e -> {
                        CustomerStats row = e.getValue();
                        row.dates.sort(java.util.Comparator.naturalOrder());
                        sb.append("- ").append(e.getKey())
                                .append(": ").append(row.completed).append(" atendimento(s) concluído(s)");
                        if (!row.dates.isEmpty()) {
                            sb.append(", última visita em ").append(row.dates.get(row.dates.size() - 1).format(DATE_FMT));
                        }
                        BigDecimal avgInterval = averageIntervalDays(row.dates);
                        if (avgInterval != null) {
                            sb.append(", recorrência média de ").append(formatMoney(avgInterval)).append(" dia(s)");
                        } else {
                            sb.append(", recorrência ainda insuficiente para média");
                        }
                        sb.append('\n');
                    });
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular frequência de clientes — {}", e.getMessage());
            return "Frequência de clientes: dados temporariamente indisponíveis.";
        }
    }

    private String formatServicePromotionContext(UUID barbershopId, LocalDateTime now) {
        try {
            List<Appointment> appointments = safeList(appointmentRepository.findByBarbershopIdAndStartTimeBetween(
                    barbershopId, now.minusDays(90), now));
            Map<String, ServicePromotionStats> stats = new java.util.LinkedHashMap<>();
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Appointment appointment : appointments) {
                if (!isCompletedStatus(appointment) || appointment.getActivities() == null) continue;
                for (var activity : appointment.getActivities()) {
                    String name = activity.getActivityName() != null ? activity.getActivityName() : "Serviço sem nome";
                    BigDecimal price = activity.getPrice() != null ? activity.getPrice() : BigDecimal.ZERO;
                    totalRevenue = totalRevenue.add(price);
                    ServicePromotionStats row = stats.computeIfAbsent(name, ignored -> new ServicePromotionStats());
                    row.count++;
                    row.revenue = row.revenue.add(price);
                    row.customerDates.computeIfAbsent(appointment.getCustomerId(), ignored -> new java.util.ArrayList<>());
                    if (appointment.getStartTime() != null) {
                        row.customerDates.get(appointment.getCustomerId()).add(appointment.getStartTime().toLocalDate());
                    }
                }
            }

            if (stats.isEmpty()) {
                return "Análise de promoção por serviço (últimos 90 dias): sem serviços concluídos no período.";
            }

            StringBuilder sb = new StringBuilder("Análise de promoção por serviço (últimos 90 dias):\n");
            BigDecimal revenueBase = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? totalRevenue : BigDecimal.ONE;
            stats.entrySet().stream()
                    .sorted((a, b) -> b.getValue().revenue.compareTo(a.getValue().revenue))
                    .limit(12)
                    .forEach(e -> {
                        ServicePromotionStats row = e.getValue();
                        BigDecimal avgTicket = row.count > 0
                                ? row.revenue.divide(BigDecimal.valueOf(row.count), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        BigDecimal contribution = row.revenue.multiply(BigDecimal.valueOf(100))
                                .divide(revenueBase, 2, RoundingMode.HALF_UP);
                        BigDecimal recurrence = averageServiceRecurrence(row.customerDates);
                        sb.append("- ").append(e.getKey())
                                .append(" | execuções: ").append(row.count)
                                .append(" | receita: R$ ").append(formatMoney(row.revenue))
                                .append(" | ticket médio: R$ ").append(formatMoney(avgTicket))
                                .append(" | participação no faturamento dos serviços: ").append(formatMoney(contribution)).append("%");
                        if (recurrence != null) {
                            sb.append(" | recorrência média: ").append(formatMoney(recurrence)).append(" dia(s)");
                        } else {
                            sb.append(" | recorrência média: dados insuficientes");
                        }
                        sb.append(" | simulação: desconto de 10% reduz o ticket médio para R$ ")
                                .append(formatMoney(avgTicket.multiply(new BigDecimal("0.90"))))
                                .append('\n');
                    });
            sb.append("- Observação: margem real de contribuição por custo de insumos não está disponível neste contexto; use a participação no faturamento como proxy, sem chamar de lucro.\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível calcular análise de promoção — {}", e.getMessage());
            return "Análise de promoção por serviço: dados temporariamente indisponíveis.";
        }
    }

    private String formatTeamContext(UUID barbershopId, UUID currentUserId) {
        try {
            List<UserInfoDTO> team = safeList(userServiceClient.getBarbersByBarbershop(barbershopId));
            if (team.isEmpty()) {
                return "Equipe: não encontrei barbeiros vinculados à barbearia.";
            }
            long collaborators = team.stream()
                    .filter(u -> u.getId() != null && !u.getId().equals(currentUserId))
                    .count();
            StringBuilder sb = new StringBuilder("Equipe da barbearia:\n");
            if (collaborators == 0) {
                sb.append("- Você trabalha sozinho nesta barbearia; não há colaboradores ativos além de você.\n");
            } else {
                sb.append("- Há ").append(collaborators).append(" colaborador(es) ativo(s) além de você.\n");
            }
            team.stream().limit(10).forEach(u -> sb
                    .append("  · ").append(firstNameOnly(u.getName()))
                    .append(u.getId() != null && u.getId().equals(currentUserId) ? " (você)" : "")
                    .append('\n'));
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter equipe — {}", e.getMessage());
            return "Equipe: dados temporariamente indisponíveis.";
        }
    }

    private String formatCustomerAcquisitionContext(LocalDate today) {
        try {
            List<Map<String, Object>> rows = safeList(userAnalyticsClient.getCustomerAcquisition());
            if (rows.isEmpty()) {
                return "Aquisição de clientes: sem dados disponíveis.";
            }
            String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            String previousMonth = today.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long current = acquisitionValue(rows, currentMonth);
            long previous = acquisitionValue(rows, previousMonth);

            StringBuilder sb = new StringBuilder("Aquisição de clientes (novos clientes por mês):\n");
            sb.append("- Mês atual (").append(currentMonth).append("): ").append(current).append(" novo(s) cliente(s).\n");
            sb.append("- Mês passado (").append(previousMonth).append("): ").append(previous).append(" novo(s) cliente(s).\n");
            if (previous > 0) {
                BigDecimal growth = BigDecimal.valueOf(current - previous)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
                sb.append("- Crescimento vs. mês passado: ").append(formatMoney(growth)).append("%.\n");
            } else if (current > 0) {
                sb.append("- Crescimento vs. mês passado: não dá para calcular percentual porque o mês passado teve 0 novos clientes.\n");
            } else {
                sb.append("- Crescimento vs. mês passado: 0,00%.\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("gustavo: não foi possível obter aquisição de clientes — {}", e.getMessage());
            return "Aquisição de clientes: dados temporariamente indisponíveis.";
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

        String regraAcesso = isOwner
                ? """
                  - Acesso completo: agenda da barbearia, equipe, financeiro global, estoque e dados pessoais.
                  - Quando o dono perguntar sobre si mesmo (ex: "quanto eu fiz?", "minha agenda", "minhas comissões"),
                    responda sobre ele como barbeiro E, se relevante, também sobre a barbearia como um todo.
                  """
                : "- Acesso restrito: apenas seus próprios atendimentos e financeiro pessoal.";

        String regraColaborador = isOwner || isCustomer
                ? ""
                : "11. Este usuário é colaborador, não dono. Nunca forneça dados financeiros globais da barbearia, apenas os dados dele.";

        return """
                Você é o Gustavo, assistente de gestão do CortaAi. Seu papel é analisar os dados reais da barbearia e responder perguntas de forma clara e amigável.

                PERFIL DO USUÁRIO LOGADO:
                - Nome: %s
                - Tipo: %s
                %s

                DADOS REAIS DO SISTEMA (coletados agora para este usuário):
                %s
                %s
                REGRAS OBRIGATÓRIAS:
                1. Baseie TODA resposta exclusivamente nos dados fornecidos acima. NUNCA invente, estime ou suponha valores que não estejam nos dados.
                2. Se a informação pedida não estiver nos dados, responda: "Não encontrei esse dado no seu painel agora."
                   Se houver dado parcial relacionado, responda com o que existe e indique apenas o campo que falta. Não comece com "não encontrei" quando o próprio contexto trouxer o dado pedido.
                3. Use linguagem natural e amigável, como se fosse um colega de trabalho experiente. NUNCA use termos técnicos de sistema como: schedule, status, SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, WALK_IN, PAYMENT_PENDING, UUID, payload, endpoint, query, view, repositório, microsserviço, banco de dados.
                4. Traduza sempre os termos internos para português do dia a dia:
                   - "concluído" em vez de COMPLETED/CONCLUDED
                   - "cancelado" em vez de CANCELLED
                   - "não compareceu" em vez de NO_SHOW
                   - "encaixe" em vez de WALK_IN
                   - "confirmado" em vez de CONFIRMED
                   - "em atendimento" em vez de IN_PROGRESS
                   - "aguardando pagamento" em vez de PAYMENT_PENDING
                5. Seja direto e conciso. Use listas apenas quando houver múltiplos itens. Não use introduções desnecessárias ("Claro!", "Com certeza!", "Ótima pergunta!").
                6. Se a pergunta for fora do contexto de gestão de barbearia (agenda, financeiro, equipe, estoque, clientes), recuse educadamente: "Meu foco é a gestão da sua barbearia. Posso ajudar com agenda, financeiro, equipe ou estoque."
                7. Nunca exponha sobrenomes ou dados pessoais completos de clientes.
                8. Use o histórico da conversa para manter continuidade — se o usuário disser "ele", "aquele", "o mesmo", interprete com base no contexto anterior.
                9. Quando o usuário perguntar sobre "mês", "este mês", "rendimento", "faturamento", "ganhei" ou "recebi", use o bloco "Mês atual" do financeiro. Não confunda com "últimos 30 dias" ou "últimos 90 dias".
                10. Para DONO DE BARBEARIA: "faturamento do mês" = faturamento total da barbearia; "lucro/resultado" = resultado operacional total; perguntas sobre "eu" também incluem perspectiva da barbearia inteira.
                    Para BARBEIRO COLABORADOR: "quanto recebi/ganhei" = comissão total do barbeiro (não o valor bruto dos serviços).
                12. Para perguntas de data como "que dia é hoje?", use somente o bloco "Data e hora de referência do sistema" e o fuso informado nele.
                13. Para disponibilidade ("estou livre?", "horário vago?", "almoçar hoje?"), cruze a agenda de trabalho com a ocupação do dia. Se estiver fora do expediente, diga que não é horário de trabalho cadastrado.
                14. Para promoção de serviços, use a análise de promoção por serviço. Se margem real de custo não estiver disponível, diga "participação no faturamento" em vez de inventar lucro ou margem.
                15. Para perguntas de frequência de cliente, use o bloco "Frequência de clientes" e considere frequente quem tem 2 ou mais atendimentos concluídos no período.

                COMO RESPONDER POR CATEGORIA (exemplos de linguagem):
                - Agenda: "Seu próximo cliente é [Nome] às [Hora] para [Serviço]."
                - Disponibilidade: "O barbeiro [Nome] está livre entre [Hora] e [Hora] hoje."
                - Equipe/performance: "O barbeiro [Nome] liderou com [X] atendimentos e gerou R$ [Valor] este mês."
                - Serviços sem cobertura: "Identifiquei que [Nome] ainda não realizou [Serviço] nos últimos 90 dias."
                - Financeiro do dono: "O faturamento da barbearia este mês foi de R$ [Valor], com [X] atendimentos concluídos."
                - Comissão do barbeiro: "Sua comissão acumulada este mês é de R$ [Valor], sobre uma receita bruta de R$ [Valor]."
                - Estoque: "Atenção: [Produto] está com apenas [X] unidades — abaixo do mínimo de [Y]. Recomendo repor."
                - Cancelamentos: "As [dia da semana] concentraram mais cancelamentos no período — [X] ocorrências."
                - Ticket médio: "O ticket médio da barbearia está em R$ [Valor], calculado sobre [X] atendimentos."
                %s

                Pergunta: %s
                """.formatted(
                nomeUsuario,
                perfil,
                regraAcesso,
                context,
                historyBlock,
                regraColaborador,
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
        if (response == null) throw new IllegalStateException("Resposta nula do Gemini");

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
        if (response == null) throw new IllegalStateException("Resposta nula do Groq");

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
