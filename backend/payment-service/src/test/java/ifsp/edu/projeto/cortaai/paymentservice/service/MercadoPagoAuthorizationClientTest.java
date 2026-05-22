package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.SaveMpCredentialsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MercadoPagoAuthorizationClientTest {

    private MercadoPagoAuthorizationClient client;

    @BeforeEach
    void setUp() {
        client = new MercadoPagoAuthorizationClient();
        ReflectionTestUtils.setField(client, "mpAuthorizationApiBaseUrl", "https://api.mercadolibre.test/");
        ReflectionTestUtils.setField(client, "mpClientId", "client-id");
    }

    @Test
    void shouldIgnoreMissingCredentials() {
        assertThatCode(() -> client.revokeSellerAuthorization(null)).doesNotThrowAnyException();
        assertThatCode(() -> client.revokeSellerAuthorization(new SaveMpCredentialsDTO(null, null, "seller", null)))
                .doesNotThrowAnyException();
        assertThatCode(() -> client.revokeSellerAuthorization(new SaveMpCredentialsDTO("token", null, " ", null)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRevocationWhenClientIdIsMissing() {
        ReflectionTestUtils.setField(client, "mpClientId", " ");

        assertThatThrownBy(() -> client.revokeSellerAuthorization(
                new SaveMpCredentialsDTO("access-token", "refresh-token", "seller-123", "public-key")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode().value()).isEqualTo(502))
                .hasMessageContaining("Credenciais Mercado Pago incompletas");
    }

    @Test
    void shouldNormalizeEncodeMaskAndDetectBlankValues() {
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "normalizedAuthorizationApiBaseUrl"))
                .isEqualTo("https://api.mercadolibre.test");
        ReflectionTestUtils.setField(client, "mpAuthorizationApiBaseUrl", " ");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "normalizedAuthorizationApiBaseUrl"))
                .isEqualTo("https://api.mercadolibre.com");

        assertThat((String) ReflectionTestUtils.invokeMethod(client, "encode", "seller id"))
                .isEqualTo("seller+id");
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(client, "isBlank", " "))
                .isTrue();
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(client, "isBlank", "seller"))
                .isFalse();
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "maskIdentifier", (Object) null))
                .isEqualTo("***");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "maskIdentifier", "123456"))
                .isEqualTo("***");
        assertThat((String) ReflectionTestUtils.invokeMethod(client, "maskIdentifier", "123456789"))
                .isEqualTo("1234...89");
    }
}
