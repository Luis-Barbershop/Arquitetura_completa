package ifsp.edu.projeto.cortaai.scheduleservice.service;

import ifsp.edu.projeto.cortaai.scheduleservice.dto.ActivityInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatRequestDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.AiChatResponseDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.dto.UserInfoDTO;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.BarbershopServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.PaymentServiceClient;
import ifsp.edu.projeto.cortaai.scheduleservice.feign.ProductServiceClient;
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
import java.time.LocalDateTime;
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
        when(appointmentRepository.findCompletedByBarbershop(eq(shopId), any(), any()))
                .thenReturn(List.of(appointment(shopId, barberId, ownerId, AppointmentStatus.COMPLETED, "Carla Mendes", "Bruno Costa", "90.00")));
        when(paymentServiceClient.getBarberPerformance(shopId)).thenReturn(List.of(
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
                .contains("Agendamentos futuros")
                .contains("Situação do estoque")
                .contains("Receita últimos 90 dias")
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
                .contains("Agendamentos futuros")
                .contains("apenas os dados dele");
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

    @Test
    void shouldKeepOwnerContextResilientWhenExternalAnalyticsFail() {
        UUID ownerId = UUID.randomUUID();
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
        when(paymentServiceClient.getBarberPerformance(shopId)).thenThrow(new RuntimeException("payment off"));
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

    private void setProvider(String field, String value) {
        ReflectionTestUtils.setField(service, field, value);
    }

    private UserInfoDTO user(UUID id, String type, UUID shopId) {
        UserInfoDTO user = new UserInfoDTO();
        user.setId(id);
        user.setName(type.equals("CUSTOMER") ? "Ana Cliente" : "Bruno Barbeiro");
        user.setUserType(type);
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
}
