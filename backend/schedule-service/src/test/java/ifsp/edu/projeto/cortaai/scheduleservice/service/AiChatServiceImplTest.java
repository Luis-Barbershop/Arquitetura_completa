package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.CommissionRuleInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.DayScheduleDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.WorkBlockDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.PaymentServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserAnalyticsClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.model.Appointment;
import ifsp.edu.projeto.cortaai.scheduleservice.model.AppointmentActivity;
import ifsp.edu.projeto.cortaai.scheduleservice.model.analytics.VBarberSkillMatrix;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AiChatMode;
import ifsp.edu.projeto.cortaai.scheduleservice.model.enums.AppointmentStatus;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.AppointmentRepository;
import ifsp.edu.projeto.cortaai.scheduleservice.repository.analytics.VBarberSkillMatrixRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private VBarberSkillMatrixRepository vBarberSkillMatrixRepository;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private UserAnalyticsClient userAnalyticsClient;
    @Mock
    private BarbershopServiceClient barbershopServiceClient;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private PaymentServiceClient paymentServiceClient;
    @Mock
    private ChatHistoryService chatHistoryService;
    @Mock
    private RestTemplate restTemplate;

    private AiChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiChatServiceImpl(
                appointmentRepository,
                vBarberSkillMatrixRepository,
                userServiceClient,
                userAnalyticsClient,
                barbershopServiceClient,
                productServiceClient,
                paymentServiceClient,
                chatHistoryService,
                restTemplate
        );
        when(chatHistoryService.getHistory("firebase-uid")).thenReturn(List.of(Map.of("role", "user", "content", "E ontem?")));
        when(chatHistoryService.formatHistoryForPrompt(any())).thenReturn("HISTÓRICO DA CONVERSA ATUAL:\nUsuário: E ontem?\n");
        setProvider("geminiUrl", "https://gemini.test/generate");
        setProvider("groqUrl", "https://groq.test/chat");
        setProvider("groqModel", "llama-test");
        setProvider("openrouterUrl", "https://openrouter.test/chat");
        setProvider("openrouterModel", "router-test");
        setProvider("cohereUrl", "https://cohere.test/chat");
        setProvider("cohereModel", "command-test");
    }

    @Test
    void shouldReturnFallbackWhenUserCannotBeResolvedAndNoProviderIsConfigured() {
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenThrow(new RuntimeException("user offline"));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "BARBER",
                new AiChatRequestDTO("Como esta minha agenda?", AiChatMode.PREVIEW)
        );

        assertThat(response.source()).isEqualTo("fallback");
        assertThat(response.message()).contains("temporariamente indisponível");
        verify(appointmentRepository, never()).findUpcomingByBarberId(any(), any());
        verify(chatHistoryService, never()).appendTurn(anyString(), anyString(), anyString());
    }

    @Test
    void shouldBuildOwnerPreviewContextAndUseGroqProvider() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any()))
                .thenReturn(List.of(appointment(shopId, barberId, ownerId, AppointmentStatus.CONFIRMED, "Ana Silva", "Bruno Costa", "70.00")));
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString()))
                .thenReturn(List.of(skill("Bruno Costa", "Corte", 4L, "280.00")));
        when(barbershopServiceClient.getAllActivities(shopId))
                .thenReturn(List.of(activityInfo(shopId, "Corte"), activityInfo(shopId, "Barba")));
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of(
                Map.of("productName", "Pomada", "category", "Finalizador", "currentStock", 0, "predictedMinimum", 3, "requiresRestock", true),
                Map.of("productName", "Shampoo", "currentStock", 12, "requiresRestock", false)
        ));
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(
                        Map.of("totalServiceRevenue", "413.40", "serviceRevenue", "350.00", "walkInRevenue", "63.40",
                                "productExpenses", "50.00", "inventoryAssetValue", "120.00", "operationalResultWithWalkIn", "363.40",
                                "approvedCount", 5, "pendingCount", 1, "cancelledCount", 0, "walkInAppointmentsCount", 1),
                        Map.of("totalServiceRevenue", "90.00", "operationalResultWithWalkIn", "90.00",
                                "approvedCount", 1, "walkInAppointmentsCount", 0)
                );
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any())).thenReturn(List.of(
                Map.of("barberName", "Bruno", "totalAppointments", 6, "generatedRevenue", "540.00", "contributionPercentage", "70")
        ));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Agenda cheia hoje.")))));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "BARBER",
                new AiChatRequestDTO("Resumo da barbearia", AiChatMode.PREVIEW)
        );

        assertThat(response.source()).isEqualTo("groq");
        assertThat(response.message()).isEqualTo("Agenda cheia hoje.");
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("Próximos atendimentos agendados")
                .contains("Situação do estoque")
                .contains("Financeiro do painel do dono")
                .contains("Faturamento total: R$ 413,40")
                .contains("Ranking de barbeiros no mês atual")
                .contains("Serviços não executados por barbeiro");
        verify(chatHistoryService).appendTurn("firebase-uid", "Resumo da barbearia", "Agenda cheia hoje.");
    }

    @Test
    void shouldBuildCustomerConsolidatedContextAndUseGeminiProvider() {
        UUID customerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        setProvider("geminiApiKey", "gemini-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(customerId, "CUSTOMER", null));
        when(appointmentRepository.findCompletedByCustomerId(eq(customerId), any(), any()))
                .thenReturn(List.of(appointment(shopId, barberId, customerId, AppointmentStatus.COMPLETED, "Ana Cliente", "Bruno Costa", "80.00")));
        when(appointmentRepository.findCancelledByCustomerId(eq(customerId), any(), any()))
                .thenReturn(List.of(appointment(shopId, barberId, customerId, AppointmentStatus.CANCELLED, "Ana Cliente", "Bruno Costa", "0.00")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of(
                        "candidates", List.of(Map.of(
                                "content", Map.of("parts", List.of(Map.of("text", "Voce fez um corte.")))
                        ))
                ));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "CUSTOMER",
                new AiChatRequestDTO("O que eu fiz?", AiChatMode.CONSOLIDATED)
        );

        assertThat(response.source()).isEqualTo("gemini");
        assertThat(response.message()).isEqualTo("Voce fez um corte.");
        verify(vBarberSkillMatrixRepository, never()).findByBarbershopId(anyString());
        verify(productServiceClient, never()).getStockHealth(any());
        verify(chatHistoryService).appendTurn("firebase-uid", "O que eu fiz?", "Voce fez um corte.");
    }

    @Test
    void shouldBuildBarberPreviewContextWithoutOwnerDataAndUseOpenRouterProvider() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        setProvider("openrouterApiKey", "openrouter-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(barberId, "BARBER", null));
        when(appointmentRepository.findUpcomingByBarberId(eq(barberId), any()))
                .thenReturn(List.of(appointment(shopId, barberId, UUID.randomUUID(), AppointmentStatus.SCHEDULED, "Cliente Um", "Barbeiro Solo", "50.00")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Voce tem um horario futuro.")))));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "BARBER",
                new AiChatRequestDTO("Tenho agenda?", AiChatMode.PREVIEW)
        );

        assertThat(response.source()).isEqualTo("openrouter");
        assertThat(response.message()).isEqualTo("Voce tem um horario futuro.");
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("BARBEIRO COLABORADOR")
                .contains("Próximos atendimentos agendados")
                .contains("apenas os dados dele");
        verify(productServiceClient, never()).getStockHealth(any());
    }

    @Test
    void shouldTreatLinkedBarberAsCollaboratorWhenRoleIsNotOwner() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UserInfoDTO barber = user(barberId, "BARBER", shopId);
        barber.setRole("ROLE_BARBER");
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(barber);
        when(appointmentRepository.findCompletedByBarberId(eq(barberId), any(), any()))
                .thenReturn(List.of(appointment(shopId, barberId, UUID.randomUUID(), AppointmentStatus.COMPLETED, "Cliente Um", "Barbeiro Linkado", "70.00")));
        when(paymentServiceClient.getMyBarberSummary(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(
                        Map.of("barberTotalCommission", "35.00", "barberServiceCommission", "35.00", "barberWalkInCommission", "0.00",
                                "grossTotalRevenue", "70.00", "barbershopTotalCommission", "35.00",
                                "approvedCount", 1, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0),
                        Map.of("barberTotalCommission", "35.00", "grossTotalRevenue", "70.00", "barbershopTotalCommission", "35.00")
                );
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Sua comissão é R$ 35,00.")))));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "BARBER",
                new AiChatRequestDTO("qual foi meu rendimento do mes?", AiChatMode.CONSOLIDATED)
        );

        assertThat(response.source()).isEqualTo("groq");
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("BARBEIRO COLABORADOR")
                .contains("Financeiro do painel do barbeiro")
                .contains("Comissão total do barbeiro: R$ 35,00")
                .doesNotContain("Financeiro do painel do dono");
        verify(paymentServiceClient, never()).getMyShopOverview(anyString(), any(), any(), any());
        verify(productServiceClient, never()).getStockHealth(any());
    }

    @Test
    void shouldFallbackFromUnavailableProvidersUntilCohereSucceeds() {
        UUID customerId = UUID.randomUUID();
        setProvider("geminiApiKey", "gemini-token");
        setProvider("groqApiKey", "groq-token");
        setProvider("openrouterApiKey", "openrouter-token");
        setProvider("cohereApiKey", "cohere-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(customerId, "CUSTOMER", null));
        when(appointmentRepository.findUpcomingByCustomerId(eq(customerId), any())).thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("gemini off"))
                .thenThrow(new RuntimeException("groq off"))
                .thenThrow(new RuntimeException("openrouter off"))
                .thenReturn(Map.of("message", Map.of("content", List.of(Map.of("text", "Cohere respondeu.")))));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "CUSTOMER",
                new AiChatRequestDTO("Proximos horarios", AiChatMode.PREVIEW)
        );

        assertThat(response.source()).isEqualTo("cohere");
        assertThat(response.message()).isEqualTo("Cohere respondeu.");
        verify(restTemplate, org.mockito.Mockito.times(4)).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    // ── Novos testes: linguagem natural e categorias de negócio ──────────────

    @Test
    void shouldTranslateStatusToNaturalLanguageInPreviewContext() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));

        // agendamentos com status variados
        List<Appointment> agendamentos = List.of(
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.CONFIRMED, "Maria", "Bruno", "60.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.IN_PROGRESS, "José", "Bruno", "60.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.SCHEDULED, "Pedro", "Bruno", "60.00")
        );
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any())).thenReturn(agendamentos);
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "180.00", "operationalResultWithWalkIn", "180.00",
                        "approvedCount", 3, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Agenda ok.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Agenda de hoje?", AiChatMode.PREVIEW));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        // Deve conter termos em português no contexto dos agendamentos
        assertThat(prompt).contains("confirmado");
        assertThat(prompt).contains("em atendimento");
        assertThat(prompt).contains("agendado");
        // Regras de linguagem estão presentes
        assertThat(prompt).contains("NUNCA use termos técnicos de sistema");
    }

    @Test
    void shouldIncludeNaturalLanguageRulesInPromptProhibitingTechnicalTerms() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any())).thenReturn(List.of());
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "0.00", "operationalResultWithWalkIn", "0.00",
                        "approvedCount", 0, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Sem dados.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Resumo", AiChatMode.PREVIEW));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        // Prompt deve instruir a evitar termos técnicos
        assertThat(prompt).contains("NUNCA use termos técnicos de sistema");
        assertThat(prompt).contains("WALK_IN");  // aparece na lista de termos proibidos
        assertThat(prompt).contains("encaixe");  // aparece como tradução
        assertThat(prompt).contains("DONO DE BARBEARIA");
    }

    @Test
    void shouldIncludeOwnerSelfReferenceRuleInPrompt() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any())).thenReturn(List.of());
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "0.00", "operationalResultWithWalkIn", "0.00",
                        "approvedCount", 0, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Resposta.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Quanto eu fiz hoje?", AiChatMode.PREVIEW));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        // Regra de auto-referência do owner deve estar no prompt
        assertThat(prompt).contains("Quando o dono perguntar sobre si mesmo");
        assertThat(prompt).contains("responda sobre ele como barbeiro E");
        // Acesso completo deve estar explícito
        assertThat(prompt).contains("Acesso completo");
    }

    @Test
    void shouldIncludeStockAlertWithCriticalAndZeroedItemsInOwnerContext() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any())).thenReturn(List.of());
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of(
                Map.of("productName", "Pomada Matte", "category", "Pomada", "currentStock", 0,
                        "predictedMinimum", 5, "requiresRestock", true),
                Map.of("productName", "Shampoo", "category", "Shampoo", "currentStock", 2,
                        "predictedMinimum", 5, "requiresRestock", true),
                Map.of("productName", "Condicionador", "category", "Condicionador", "currentStock", 20,
                        "predictedMinimum", 3, "requiresRestock", false)
        ));
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "0.00", "operationalResultWithWalkIn", "0.00",
                        "approvedCount", 0, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Precisa de reposição.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Preciso comprar algum produto?", AiChatMode.PREVIEW));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt).contains("ZERADO — estoque 0");
        assertThat(prompt).contains("Pomada Matte");
        assertThat(prompt).contains("CRÍTICO — abaixo do mínimo");
        assertThat(prompt).contains("Shampoo");
        assertThat(prompt).contains("OK — estoque suficiente");
        assertThat(prompt).contains("Condicionador");
    }

    @Test
    void shouldIncludeCancelledAppointmentsWithTranslatedStatusInOwnerContext() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID barberId2 = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));

        // agendamentos com status variados
        when(appointmentRepository.findCompletedByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of(appointment(shopId, barberId2, ownerId, AppointmentStatus.COMPLETED, "Ana", "Bruno", "60.00")));
        when(appointmentRepository.findCancelledByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of(
                        appointmentWithStatus(shopId, barberId2, ownerId, AppointmentStatus.CANCELLED, "Carlos", "Bruno", "0.00"),
                        appointmentWithStatus(shopId, barberId2, ownerId, AppointmentStatus.NO_SHOW, "Diego", "Bruno", "0.00")
                ));
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "60.00", "operationalResultWithWalkIn", "60.00",
                        "approvedCount", 1, "pendingCount", 0, "cancelledCount", 2, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Dois cancelamentos.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Quantos cancelamentos tivemos?", AiChatMode.CONSOLIDATED));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        // Status traduzidos no contexto dos agendamentos
        assertThat(prompt).contains("cancelado");
        assertThat(prompt).contains("não compareceu");
        // Regras de linguagem estão presentes — confirmam a configuração correta do prompt
        assertThat(prompt).contains("NUNCA use termos técnicos de sistema");
    }

    @Test
    void shouldIncludeSkillMatrixAndUncoveredServicesForOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any())).thenReturn(List.of());

        // Bruno fez Corte mas não fez Barba
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString()))
                .thenReturn(List.of(skill("Bruno Costa", "Corte", 10L, "700.00")));
        when(barbershopServiceClient.getAllActivities(shopId))
                .thenReturn(List.of(activityInfo(shopId, "Corte"), activityInfo(shopId, "Barba")));
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "700.00", "operationalResultWithWalkIn", "700.00",
                        "approvedCount", 10, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of(Map.of("barberName", "Bruno", "totalAppointments", 10,
                        "generatedRevenue", "700.00", "contributionPercentage", "100")));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Bruno não fez Barba.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Algum barbeiro não está fazendo algum serviço?", AiChatMode.PREVIEW));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt).contains("Bruno");
        assertThat(prompt).contains("não executou");
        assertThat(prompt).contains("Barba");
        assertThat(prompt).contains("Habilidades e serviços executados por barbeiro");
        assertThat(prompt).contains("Corte");
    }

    @Test
    void shouldIncludeExamplePhrasesByBusinessCategoryInPrompt() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findUpcomingByBarbershop(eq(shopId), any())).thenReturn(List.of());
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "0.00", "operationalResultWithWalkIn", "0.00",
                        "approvedCount", 0, "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Ok.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Dúvida", AiChatMode.PREVIEW));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        // Deve conter as seções de exemplo de resposta por categoria
        assertThat(prompt).contains("COMO RESPONDER POR CATEGORIA");
        assertThat(prompt).contains("Ticket médio");
        assertThat(prompt).contains("Estoque");
        assertThat(prompt).contains("Cancelamentos");
        assertThat(prompt).contains("encaixe");
    }

    @Test
    void shouldIncludeAdvancedDecisionContextsForOwnerQuestions() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));

        Appointment progressiva1 = appointment(shopId, ownerId, customerId, AppointmentStatus.COMPLETED, "Ana Silva", "Bruno", "300.00");
        progressiva1.setStartTime(LocalDateTime.of(2026, 5, 6, 10, 0));
        progressiva1.getActivities().iterator().next().setActivityName("Progressiva");
        Appointment progressiva2 = appointment(shopId, ownerId, customerId, AppointmentStatus.COMPLETED, "Ana Silva", "Bruno", "300.00");
        progressiva2.setStartTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        progressiva2.getActivities().iterator().next().setActivityName("Progressiva");
        Appointment cancelled = appointment(shopId, ownerId, UUID.randomUUID(), AppointmentStatus.CANCELLED, "Bia", "Bruno", "0.00");
        cancelled.setStartTime(LocalDateTime.of(2026, 5, 21, 10, 0));

        when(appointmentRepository.findCompletedByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of(progressiva1, progressiva2));
        when(appointmentRepository.findCancelledByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of(cancelled));
        when(appointmentRepository.findByBarbershopIdAndStartTimeBetween(eq(shopId), any(), any()))
                .thenReturn(List.of(progressiva1, progressiva2, cancelled));
        when(appointmentRepository.findByBarberIdAndStartTimeBetween(eq(ownerId), any(), any()))
                .thenReturn(List.of());
        when(userServiceClient.getBarberWorkSchedule(ownerId)).thenReturn(fullWeekSchedule());
        when(userServiceClient.getBarbersByBarbershop(shopId)).thenReturn(List.of(user(ownerId, "BARBER", shopId)));
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString()))
                .thenReturn(List.of(skill("Bruno", "Progressiva", 2L, "600.00")));
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of(activityInfo(shopId, "Progressiva")));
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of(
                Map.of("productName", "Creme Progressiva", "category", "Química", "currentStock", 9,
                        "predictedMinimum", 3, "requiresRestock", false)
        ));
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "600.00", "serviceRevenue", "600.00",
                        "walkInRevenue", "0.00", "productExpenses", "30.00", "inventoryAssetValue", "30.00",
                        "operationalResultWithWalkIn", "570.00", "approvedCount", 2,
                        "pendingCount", 0, "cancelledCount", 1, "walkInAppointmentsCount", 0));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        when(userAnalyticsClient.getCustomerAcquisition()).thenReturn(List.of(
                Map.of("referenceMonth", today.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")), "newCustomers", 4),
                Map.of("referenceMonth", today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")), "newCustomers", 6)
        ));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Contextos ok.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Vale promoção? Ana é frequente?", AiChatMode.CONSOLIDATED));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt)
                .contains("Data e hora de referência do sistema")
                .contains("Agenda de trabalho e disponibilidade")
                .contains("Amanhã às 12:00")
                .contains("Cancelamentos por dia da semana")
                .contains("Maior taxa")
                .contains("Frequência de clientes")
                .contains("Ana")
                .contains("Análise de promoção por serviço")
                .contains("Progressiva")
                .contains("recorrência média")
                .contains("Equipe da barbearia")
                .contains("Aquisição de clientes")
                .contains("Crescimento vs. mês passado")
                .contains("Leitura sobre estoque alto");
    }

    @Test
    void shouldNotExposeOwnerDataToCollaboratorBarber() {
        UUID barberId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UserInfoDTO collaborator = user(barberId, "BARBER", shopId);
        collaborator.setRole("ROLE_BARBER");
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(collaborator);
        when(appointmentRepository.findCompletedByBarberId(eq(barberId), any(), any())).thenReturn(List.of());
        when(paymentServiceClient.getMyBarberSummary(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(
                        Map.of("barberTotalCommission", "120.00", "barberServiceCommission", "120.00",
                                "barberWalkInCommission", "0.00", "grossTotalRevenue", "240.00",
                                "barbershopTotalCommission", "120.00", "approvedCount", 4,
                                "pendingCount", 0, "cancelledCount", 0, "walkInAppointmentsCount", 0),
                        Map.of("barberTotalCommission", "0.00", "grossTotalRevenue", "0.00",
                                "barbershopTotalCommission", "0.00")
                );
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Sua comissão é R$ 120,00.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Qual minha comissão?", AiChatMode.CONSOLIDATED));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        // Prompt deve indicar restrição de acesso ao colaborador
        assertThat(prompt).contains("BARBEIRO COLABORADOR");
        assertThat(prompt).contains("colaborador, não dono");
        // Não chama os endpoints globais da barbearia
        verify(paymentServiceClient, never()).getMyShopOverview(anyString(), any(), any(), any());
        verify(productServiceClient, never()).getStockHealth(any());
        verify(vBarberSkillMatrixRepository, never()).findByBarbershopId(anyString());
    }

    @Test
    void shouldKeepOwnerContextResilientWhenExternalAnalyticsFail() {        UUID ownerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));
        when(appointmentRepository.findCompletedByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of())
                .thenThrow(new RuntimeException("db off"));
        when(appointmentRepository.findCancelledByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString()))
                .thenThrow(new RuntimeException("view off"));
        when(barbershopServiceClient.getAllActivities(shopId)).thenThrow(new RuntimeException("barbershop off"));
        when(productServiceClient.getStockHealth(shopId)).thenThrow(new RuntimeException("product off"));
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenThrow(new RuntimeException("payment off"));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Dados parciais.")))));

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "BARBER",
                new AiChatRequestDTO("Financeiro", AiChatMode.CONSOLIDATED)
        );

        assertThat(response.source()).isEqualTo("groq");
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));
        assertThat(entityCaptor.getValue().getBody().toString())
                .contains("dados temporariamente indisponíveis")
                .contains("Nenhum atendimento concluído");
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldCoverOperationalMetricsWithAllAppointmentStatuses() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId  = UUID.randomUUID();
        UUID barberId = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));

        // Appointments com statuses variados para cobrir isCompletedStatus, isLostStatus e formatTopServices
        Appointment apCompleted = appointment(shopId, barberId, ownerId, AppointmentStatus.COMPLETED, "Ana", "Bruno", "50.00");
        apCompleted.getActivities().iterator().next().setActivityName("Corte");
        Appointment apConcluded = appointment(shopId, barberId, ownerId, AppointmentStatus.CONCLUDED, "Bia", "Bruno", "40.00");
        apConcluded.getActivities().iterator().next().setActivityName("Barba");
        Appointment apWalkIn = appointment(shopId, barberId, ownerId, AppointmentStatus.WALK_IN, "Carlos", "Bruno", "30.00");
        apWalkIn.getActivities().iterator().next().setActivityName("Corte");
        Appointment apScheduled = appointment(shopId, barberId, ownerId, AppointmentStatus.SCHEDULED, "Davi", "Bruno", "0.00");
        List<Appointment> concluidos = List.of(apCompleted, apConcluded, apWalkIn, apScheduled);

        // Cancelados com statuses para cobrir translateStatus: WALK_IN, NO_SHOW, PAYMENT_PENDING, EXPIRED, CONCLUDED
        List<Appointment> cancelados = List.of(
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.CANCELLED,        "Eva",  "Bruno", "0.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.NO_SHOW,          "Fio",  "Bruno", "0.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.PAYMENT_PENDING,  "Gio",  "Bruno", "0.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.EXPIRED,          "Heo",  "Bruno", "0.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.WALK_IN,          "Ivo",  "Bruno", "0.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.CONCLUDED,        "Jao",  "Bruno", "0.00")
        );
        // Métricas operacionais usam findByBarbershopIdAndStartTimeBetween
        List<Appointment> operacional = List.of(apCompleted, apConcluded, apWalkIn,
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.CANCELLED, "Ka", "Bruno", "0.00"),
                appointmentWithStatus(shopId, barberId, ownerId, AppointmentStatus.NO_SHOW,   "La", "Bruno", "0.00"));

        when(appointmentRepository.findCompletedByBarbershop(eq(shopId), any(), any())).thenReturn(concluidos);
        when(appointmentRepository.findCancelledByBarbershop(eq(shopId), any(), any())).thenReturn(cancelados);
        when(appointmentRepository.findByBarbershopIdAndStartTimeBetween(eq(shopId), any(), any())).thenReturn(operacional);
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(Map.of("totalServiceRevenue", "120.00", "serviceRevenue", "90.00",
                        "walkInRevenue", "30.00", "productExpenses", "0.00", "inventoryAssetValue", "0.00",
                        "operationalResultWithWalkIn", "120.00",
                        "approvedCount", 3, "pendingCount", 0, "cancelledCount", 2, "walkInAppointmentsCount", 1));
        when(paymentServiceClient.getMyShopBarberPerformance(eq("firebase-uid"), eq(shopId), any(), any()))
                .thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Métricas ok.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Resumo operacional", AiChatMode.CONSOLIDATED));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt)
                .contains("Resumo da agenda e atendimentos")
                .contains("Serviços mais executados no mês atual")
                .contains("Corte")
                .contains("Barba")
                .contains("cancelado")
                .contains("não compareceu")
                .contains("aguardando pagamento")
                .contains("expirado")
                .contains("encaixe (walk-in)");
    }

    @Test
    void shouldCoverFinancialSummaryFromAppointmentsWhenPaymentServiceReturnsNull() {
        UUID ownerId = UUID.randomUUID();
        UUID shopId  = UUID.randomUUID();
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(ownerId, "BARBER", shopId));

        // Appointments com totalPrice — usados em findCompletedByBarbershop (3 chamadas: 1 consolidado + 2 no summary)
        List<Appointment> completados = List.of(
                appointment(shopId, UUID.randomUUID(), UUID.randomUUID(), AppointmentStatus.COMPLETED, "Ana", "Bruno", "60.00"),
                appointment(shopId, UUID.randomUUID(), UUID.randomUUID(), AppointmentStatus.COMPLETED, "Carlos", "Pedro", "40.00")
        );
        when(appointmentRepository.findCompletedByBarbershop(eq(shopId), any(), any())).thenReturn(completados);
        when(appointmentRepository.findCancelledByBarbershop(eq(shopId), any(), any())).thenReturn(List.of());
        // getMyShopOverview retorna null → dispara formatFinancialSummaryFromAppointments
        when(paymentServiceClient.getMyShopOverview(eq("firebase-uid"), eq(shopId), any(), any())).thenReturn(null);
        when(vBarberSkillMatrixRepository.findByBarbershopId(shopId.toString())).thenReturn(List.of());
        when(barbershopServiceClient.getAllActivities(shopId)).thenReturn(List.of());
        when(productServiceClient.getStockHealth(shopId)).thenReturn(List.of());
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Resumo calculado.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Financeiro", AiChatMode.CONSOLIDATED));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt)
                .contains("Resumo financeiro (baseado em atendimentos concluídos)")
                .contains("Receita últimos 30 dias")
                .contains("Receita últimos 90 dias")
                .contains("Ticket médio por atendimento")
                .contains("Receita por barbeiro")
                .contains("Bruno")
                .contains("Pedro");
    }

    @Test
    void shouldCoverBarberCommissionContextWithRulesWhenPaymentReturnsNull() {
        UUID barberId  = UUID.randomUUID();
        UUID shopId    = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        UserInfoDTO collaborator = user(barberId, "BARBER", shopId);
        collaborator.setRole("ROLE_BARBER");
        setProvider("groqApiKey", "groq-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(collaborator);

        Appointment ap = appointment(shopId, barberId, UUID.randomUUID(), AppointmentStatus.COMPLETED, "Ana", "Bruno", "80.00");
        AppointmentActivity act = ap.getActivities().iterator().next();
        act.setActivityId(activityId);
        act.setActivityName("Corte Premium");
        when(appointmentRepository.findCompletedByBarberId(eq(barberId), any(), any())).thenReturn(List.of(ap));
        // Payment retorna null → dispara formatBarberCommissionContext
        when(paymentServiceClient.getMyBarberSummary(eq("firebase-uid"), eq(shopId), any(), any())).thenReturn(null);
        // Regras de comissão do barbeiro
        CommissionRuleInfoDTO rule = new CommissionRuleInfoDTO(
                UUID.randomUUID(), activityId, "Corte Premium", new java.math.BigDecimal("40.00"));
        when(barbershopServiceClient.getBarberCommissions(shopId, barberId)).thenReturn(List.of(rule));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("choices", List.of(Map.of("message", Map.of("content", "Comissão: R$ 32,00.")))));

        service.chat("firebase-uid", "BARBER", new AiChatRequestDTO("Minha comissão do mês", AiChatMode.CONSOLIDATED));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt)
                .contains("Resumo financeiro pessoal")
                .contains("Receita bruta gerada")
                .contains("Comissão líquida")
                .contains("Corte Premium")
                .contains("40");
    }

    @Test
    void shouldCoverCustomerPreviewContextWithActivitiesAndNullBarberName() {
        UUID customerId = UUID.randomUUID();
        UUID shopId     = UUID.randomUUID();
        UUID barberId   = UUID.randomUUID();
        setProvider("geminiApiKey", "gemini-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(customerId, "CUSTOMER", null));

        // Appointment com barberName=null (cobre firstNameOnly null) e com atividade (cobre lambda de serviços)
        Appointment ap = appointment(shopId, barberId, customerId, AppointmentStatus.SCHEDULED, "Ana Cliente", null, "60.00");
        ap.getActivities().iterator().next().setActivityName("Hidratação Capilar");
        when(appointmentRepository.findUpcomingByCustomerId(eq(customerId), any())).thenReturn(List.of(ap));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", "Você tem um horário amanhã.")))))));

        AiChatResponseDTO response = service.chat(
                "firebase-uid", "CUSTOMER", new AiChatRequestDTO("Tenho horário marcado?", AiChatMode.PREVIEW));

        assertThat(response.source()).isEqualTo("gemini");
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String prompt = captor.getValue().getBody().toString();

        assertThat(prompt)
                .contains("Seus próximos atendimentos agendados")
                .contains("serviço: Hidratação Capilar")
                .contains("barbeiro: —");  // null barberName → firstNameOnly retorna "—"
    }

    @Test
    void shouldFallbackWhenAllProvidersReturnNullResponse() {
        UUID customerId = UUID.randomUUID();
        setProvider("geminiApiKey", "gemini-token");
        setProvider("groqApiKey", "groq-token");
        setProvider("openrouterApiKey", "openrouter-token");
        setProvider("cohereApiKey", "cohere-token");
        when(userServiceClient.getUserByFirebaseUid("firebase-uid")).thenReturn(user(customerId, "CUSTOMER", null));
        when(appointmentRepository.findUpcomingByCustomerId(eq(customerId), any())).thenReturn(List.of());
        // Todos os provedores retornam null — dispara IllegalStateException em cada callXxx()
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(null);

        AiChatResponseDTO response = service.chat(
                "firebase-uid",
                "CUSTOMER",
                new AiChatRequestDTO("Teste null response", AiChatMode.PREVIEW)
        );

        assertThat(response.source()).isEqualTo("fallback");
        assertThat(response.message()).contains("temporariamente indisponível");
    }

    private void setProvider(String field, String value) {
        ReflectionTestUtils.setField(service, field, value);
    }

    private UserInfoDTO user(UUID id, String type, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setName(type.equals("CUSTOMER") ? "Ana Cliente" : "Bruno Barbeiro");
        user.setUserType(type);
        user.setRole(type.equals("CUSTOMER") ? "ROLE_CUSTOMER" : shopId != null ? "ROLE_OWNER" : "ROLE_BARBER");
        user.setBarbershopId(shopId);
        return user;
    }

    private Appointment appointment(UUID shopId,
                                    UUID barberId,
                                    UUID customerId,
                                    AppointmentStatus status,
                                    String customerName,
                                    String barberName,
                                    String price) {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setBarbershopId(shopId);
        appointment.setBarberId(barberId);
        appointment.setCustomerId(customerId);
        appointment.setCustomerName(customerName);
        appointment.setBarberName(barberName);
        appointment.setBarbershopName("Barbearia Teste");
        appointment.setStartTime(LocalDateTime.of(2026, 5, 22, 10, 0));
        appointment.setEndTime(LocalDateTime.of(2026, 5, 22, 11, 0));
        appointment.setStatus(status);
        appointment.setTotalPrice(new BigDecimal(price));
        AppointmentActivity activity = new AppointmentActivity();
        activity.setId(UUID.randomUUID());
        activity.setActivityId(UUID.randomUUID());
        activity.setActivityName("Corte");
        activity.setPrice(new BigDecimal(price));
        activity.setDurationMinutes(45);
        activity.setAppointment(appointment);
        appointment.getActivities().add(activity);
        return appointment;
    }

    /** Alias explícito por legibilidade — delega ao appointment() principal. */
    private Appointment appointmentWithStatus(UUID shopId, UUID barberId, UUID customerId,
                                              AppointmentStatus status, String customerName,
                                              String barberName, String price) {
        return appointment(shopId, barberId, customerId, status, customerName, barberName, price);
    }

    private VBarberSkillMatrix skill(String barberName, String activityName, Long times, String revenue) {
        VBarberSkillMatrix row = new VBarberSkillMatrix();
        ReflectionTestUtils.setField(row, "barberId", UUID.randomUUID().toString());
        ReflectionTestUtils.setField(row, "barberName", barberName);
        ReflectionTestUtils.setField(row, "activityName", activityName);
        ReflectionTestUtils.setField(row, "timesExecuted", times);
        ReflectionTestUtils.setField(row, "totalGeneratedByActivity", new BigDecimal(revenue));
        return row;
    }

    private ActivityInfoDTO activityInfo(UUID shopId, String name) {
        ActivityInfoDTO activity = new ActivityInfoDTO();
        activity.setId(UUID.randomUUID());
        activity.setBarbershopId(shopId);
        activity.setActivityName(name);
        activity.setPrice(new BigDecimal("70.00"));
        activity.setDurationMinutes(45);
        return activity;
    }

    private List<DayScheduleDTO> fullWeekSchedule() {
        return List.of(
                new DayScheduleDTO(DayOfWeek.MONDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
                new DayScheduleDTO(DayOfWeek.TUESDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
                new DayScheduleDTO(DayOfWeek.WEDNESDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
                new DayScheduleDTO(DayOfWeek.THURSDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
                new DayScheduleDTO(DayOfWeek.FRIDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(18, 0)))),
                new DayScheduleDTO(DayOfWeek.SATURDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(14, 0)))),
                new DayScheduleDTO(DayOfWeek.SUNDAY, List.of(new WorkBlockDTO(LocalTime.of(9, 0), LocalTime.of(14, 0))))
        );
    }
}
