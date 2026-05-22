package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.feign.ProductServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.ScheduleServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import ifsp.edu.projeto.cortaai.paymentservice.repository.DashboardKpiDailyRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.TransactionRepository;
import ifsp.edu.projeto.cortaai.paymentservice.repository.WebhookLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PaymentServiceWebhookSecurityTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WebhookLogRepository webhookLogRepository;
    @Mock
    private DashboardKpiDailyRepository dashboardKpiDailyRepository;
    @Mock
    private ScheduleServiceClient scheduleServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        setField("webhookReplayWindowSeconds", 300L);
    }

    @Test
    void shouldAcceptWebhookWhenSecretNotConfigured() {
        setField("webhookSecret", "");

        boolean trusted = paymentService.isWebhookTrusted("12345", null, null);

        assertTrue(trusted);
    }

    @Test
    void shouldAcceptWebhookWithValidSignatureInsideReplayWindow() {
        String secret = "segredo-teste-webhook";
        String resourceId = "12345";
        String requestId = "req-abc-001";
        long nowTs = Instant.now().getEpochSecond();

    setField("webhookSecret", secret);
    setField("webhookReplayWindowSeconds", 300L);

        String signatureHeader = buildSignatureHeader(resourceId, requestId, nowTs, secret);

        boolean trusted = paymentService.isWebhookTrusted(resourceId, signatureHeader, requestId);

        assertTrue(trusted);
    }

    @Test
    void shouldRejectWebhookWithInvalidSignature() {
        String secret = "segredo-teste-webhook";
        String resourceId = "12345";
        String requestId = "req-abc-002";
        long nowTs = Instant.now().getEpochSecond();

    setField("webhookSecret", secret);
    setField("webhookReplayWindowSeconds", 300L);

        String invalidSignature = "ts=" + nowTs + ",v1=assinatura-invalida";

        boolean trusted = paymentService.isWebhookTrusted(resourceId, invalidSignature, requestId);

        assertFalse(trusted);
    }

    @Test
    void shouldRejectWebhookWithMissingSignatureInputs() {
        setField("webhookSecret", "segredo-teste-webhook");

        assertFalse(paymentService.isWebhookTrusted(null, "ts=1,v1=abc", "req"));
        assertFalse(paymentService.isWebhookTrusted("123", null, "req"));
        assertFalse(paymentService.isWebhookTrusted("123", "ts=1,v1=abc", " "));
    }

    @Test
    void shouldRejectWebhookWithMalformedSignature() {
        setField("webhookSecret", "segredo-teste-webhook");

        assertFalse(paymentService.isWebhookTrusted("123", "bad-header", "req"));
        assertFalse(paymentService.isWebhookTrusted("123", "ts=not-a-number,v1=abc", "req"));
        assertFalse(paymentService.isWebhookTrusted("123", "ts=123", "req"));
    }

    @Test
    void shouldRejectWebhookWhenSignatureLengthDiffers() {
        String secret = "segredo-teste-webhook";
        String resourceId = "12345";
        String requestId = "req-abc-short";
        long nowTs = Instant.now().getEpochSecond();

        setField("webhookSecret", secret);

        String shortSignature = "ts=" + nowTs + ",v1=abc";

        assertFalse(paymentService.isWebhookTrusted(resourceId, shortSignature, requestId));
    }

    @Test
    void shouldRejectWebhookOutsideReplayWindow() {
        String secret = "segredo-teste-webhook";
        String resourceId = "12345";
        String requestId = "req-abc-003";
        long oldTs = Instant.now().minusSeconds(1200).getEpochSecond();

    setField("webhookSecret", secret);
    setField("webhookReplayWindowSeconds", 300L);

        String signatureHeader = buildSignatureHeader(resourceId, requestId, oldTs, secret);

        boolean trusted = paymentService.isWebhookTrusted(resourceId, signatureHeader, requestId);

        assertFalse(trusted);
    }

    private String buildSignatureHeader(String resourceId, String requestId, long ts, String secret) {
        String manifest = "id:" + resourceId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String hmac = hmacSha256Hex(manifest, secret);
        return "ts=" + ts + ",v1=" + hmac;
    }

    private void setField(String fieldName, Object value) {
        try {
            PaymentService target = Objects.requireNonNull(paymentService);
            Field field = PaymentService.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao configurar campo de teste: " + fieldName, ex);
        }
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format(Locale.ROOT, "%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Erro ao calcular assinatura no teste.", ex);
        }
    }
}
