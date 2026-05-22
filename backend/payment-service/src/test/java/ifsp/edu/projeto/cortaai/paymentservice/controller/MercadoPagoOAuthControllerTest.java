package ifsp.edu.projeto.cortaai.paymentservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MercadoPagoOAuthControllerTest {

    private MercadoPagoOAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new MercadoPagoOAuthController(mock(UserServiceClient.class), new ObjectMapper());
        ReflectionTestUtils.setField(controller, "mpClientId", "client-123");
        ReflectionTestUtils.setField(controller, "mpClientSecret", "secret-123");
        ReflectionTestUtils.setField(controller, "mpRedirectUri", "https://api.example.com/mp-callback");
        ReflectionTestUtils.setField(controller, "mpAuthBaseUrl", "https://auth.mercadopago.test");
        ReflectionTestUtils.setField(controller, "mpApiBaseUrl", "https://api.mercadopago.test");
        ReflectionTestUtils.setField(controller, "mpPostConnectRedirectUrl", "https://app.example.com/barberHome?old=true");
    }

    @Test
    void shouldBuildMercadoPagoAuthorizationUrlWhenOAuthIsEnabled() {
        UUID barberId = UUID.randomUUID();

        ResponseEntity<Map<String, String>> response = controller.getMpAuthorizationUrl(barberId);

        String url = response.getBody().get("authorizationUrl");
        assertThat(url).startsWith("https://auth.mercadopago.test/authorization");
        assertThat(url).contains("client_id=client-123");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("redirect_uri=https%3A%2F%2Fapi.example.com%2Fmp-callback");
        assertThat(url).contains("state=" + barberId);
    }

    @Test
    void shouldReturnDisabledRedirectWhenOAuthSecretIsMissing() {
        ReflectionTestUtils.setField(controller, "mpClientSecret", " ");

        ResponseEntity<Map<String, String>> response = controller.getMpAuthorizationUrl(UUID.randomUUID());

        assertThat(response.getBody().get("authorizationUrl"))
                .isEqualTo("https://app.example.com/barberHome?mpLinked=false&mpReason=oauth_disabled_in_test");
    }

    @Test
    void shouldRedirectCallbackToDisabledReasonWhenOAuthSecretIsMissing() {
        UUID barberId = UUID.randomUUID();
        ReflectionTestUtils.setField(controller, "mpClientSecret", null);

        ResponseEntity<Void> response = controller.handleMpCallback("code-123", barberId.toString());

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation())
                .isEqualTo(URI.create("https://app.example.com/barberHome?mpLinked=false&mpReason=oauth_disabled_in_test"));
    }

    @Test
    void shouldRejectInvalidCallbackState() {
        assertThatThrownBy(() -> controller.handleMpCallback("code-123", "not-a-uuid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Parâmetro state inválido.");
    }

    @Test
    void shouldMaskAndSanitizeSensitiveOAuthValues() {
        assertThat((String) ReflectionTestUtils.invokeMethod(controller, "maskIdentifier", (Object) null))
                .isEqualTo("***");
        assertThat((String) ReflectionTestUtils.invokeMethod(controller, "maskIdentifier", "123456"))
                .isEqualTo("***");
        assertThat((String) ReflectionTestUtils.invokeMethod(controller, "maskIdentifier", "123456789"))
                .isEqualTo("1234...89");

        assertThat((String) ReflectionTestUtils.invokeMethod(controller, "sanitizeMessage", (Object) null))
                .isEqualTo("n/a");
        assertThat((String) ReflectionTestUtils.invokeMethod(controller, "sanitizeMessage", "erro comum"))
                .isEqualTo("erro comum");
        assertThat((String) ReflectionTestUtils.invokeMethod(controller, "sanitizeMessage",
                "token:abc123 secret=super value"))
                .isEqualTo("token=*** secret=*** value");
    }
}
