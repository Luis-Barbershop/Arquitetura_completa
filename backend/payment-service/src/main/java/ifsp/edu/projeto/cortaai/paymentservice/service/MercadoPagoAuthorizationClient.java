package ifsp.edu.projeto.cortaai.paymentservice.service;

import ifsp.edu.projeto.cortaai.paymentservice.dto.SaveMpCredentialsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class MercadoPagoAuthorizationClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Value("${mercadopago.authorization-api-base-url}")
    private String mpAuthorizationApiBaseUrl;

    @Value("${mercadopago.client-id}")
    private String mpClientId;

    public void revokeSellerAuthorization(SaveMpCredentialsDTO credentials) {
        if (credentials == null || isBlank(credentials.mpAccessToken()) || isBlank(credentials.mpUserId())) {
            return;
        }
        if (isBlank(mpClientId)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Credenciais Mercado Pago incompletas para revogar a autorizacao.");
        }

        try {
            URI uri = URI.create(normalizedAuthorizationApiBaseUrl()
                    + "/users/" + encode(credentials.mpUserId())
                    + "/applications/" + encode(mpClientId));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + credentials.mpAccessToken())
                    .DELETE()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                log.info("event=mp-oauth-authorization-revoked mpUserId={}", maskIdentifier(credentials.mpUserId()));
                return;
            }
            if (status == 404 || status == 410) {
                log.info("event=mp-oauth-authorization-already-revoked mpUserId={} status={}",
                        maskIdentifier(credentials.mpUserId()),
                        status);
                return;
            }

            log.warn("event=mp-oauth-authorization-revoke-failed mpUserId={} status={}",
                    maskIdentifier(credentials.mpUserId()),
                    status);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel revogar a autorizacao no Mercado Pago.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("event=mp-oauth-authorization-revoke-error cause={}", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Nao foi possivel revogar a autorizacao no Mercado Pago.", ex);
        }
    }

    private String normalizedAuthorizationApiBaseUrl() {
        String baseUrl = isBlank(mpAuthorizationApiBaseUrl)
                ? "https://api.mercadolibre.com"
                : mpAuthorizationApiBaseUrl;
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String maskIdentifier(Object value) {
        if (value == null) {
            return "***";
        }

        String normalized = value.toString().trim();
        if (normalized.length() <= 6) {
            return "***";
        }

        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 2);
    }
}
