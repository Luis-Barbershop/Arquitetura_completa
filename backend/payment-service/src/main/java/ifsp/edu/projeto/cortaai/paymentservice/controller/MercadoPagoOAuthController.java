package ifsp.edu.projeto.cortaai.paymentservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifsp.edu.projeto.cortaai.paymentservice.dto.SaveMpCredentialsDTO;
import ifsp.edu.projeto.cortaai.paymentservice.exception.ApiErrorResponse;
import ifsp.edu.projeto.cortaai.paymentservice.feign.UserServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Controller responsável pelo fluxo OAuth do Mercado Pago para barbeiros (Marketplace).
 *
 * Fluxo:
 *  1. Barbeiro clica em "Vincular Mercado Pago" → front chama GET /api/payments/mp-connect
 *  2. API retorna a URL de autorização do MP → front redireciona o barbeiro
 *  3. MP chama GET /api/payments/mp-callback?code=...&state=...
 *  4. API troca o code pelo access_token e salva no user-service via Feign
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "NOVO: Endpoints para criação e consulta de pagamentos de agendamentos via Mercado Pago")
public class MercadoPagoOAuthController {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final UserServiceClient userServiceClient;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.client-id}")
    private String mpClientId;

    @Value("${mercadopago.client-secret}")
    private String mpClientSecret;

    @Value("${mercadopago.redirect-uri}")
    private String mpRedirectUri;

        @Value("${mercadopago.auth-base-url}")
        private String mpAuthBaseUrl;

                @Value("${mercadopago.api-base-url}")
                private String mpApiBaseUrl;

                @Value("${mercadopago.post-connect-redirect-url}")
                private String mpPostConnectRedirectUrl;

    // ─── GET /mp-connect ────────────────────────────────────────────────────────

    @Operation(
            summary = "Iniciar vinculação Mercado Pago (OAuth)",
            description = """
                    Retorna a URL de autorização do Mercado Pago para que o barbeiro
                    possa vincular sua conta ao marketplace CortaAI.
                    
                    O front-end deve redirecionar o barbeiro para a URL retornada.
                    O parâmetro `state` deve ser o UUID do barbeiro logado (X-User-Id),
                    para que o callback saiba a qual barbeiro associar as credenciais.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL de autorização gerada com sucesso",
                    content = @Content(schema = @Schema(example = "{\"authorizationUrl\": \"https://auth.mercadopago.com.br/authorization?...\"}"))),
            @ApiResponse(responseCode = "400", description = "Parâmetro state (barberId) ausente",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/mp-connect")
    public ResponseEntity<java.util.Map<String, String>> getMpAuthorizationUrl(
            @Parameter(description = "ID do barbeiro logado (X-User-Id injetado pelo Gateway)", hidden = true)
            @RequestParam("state") UUID barberId) {

        if (!isOAuthFlowEnabled()) {
            log.warn("event=mp-oauth-disabled-environment barberId={} reason=missing_client_secret", maskIdentifier(barberId));
            return ResponseEntity.ok(java.util.Map.of(
                    "authorizationUrl",
                    buildPostConnectRedirectUrl(false, "oauth_disabled_in_test")
            ));
        }

        String url = mpAuthBaseUrl + "/authorization" +
                "?client_id=" + mpClientId +
                "&response_type=code" +
                "&platform_id=mp" +
                "&redirect_uri=" + URLEncoder.encode(mpRedirectUri, StandardCharsets.UTF_8) +
                "&state=" + barberId.toString();

        return ResponseEntity.ok(java.util.Map.of("authorizationUrl", url));
    }

    // ─── GET /mp-callback ───────────────────────────────────────────────────────

    @Operation(
            summary = "Callback OAuth Mercado Pago",
            description = """
                    Endpoint chamado pelo Mercado Pago após o barbeiro autorizar o acesso.
                    
                    - Troca o `code` pelo `access_token` via API do MP.
                    - Salva `mpAccessToken`, `mpRefreshToken`, `mpUserId` e `mpPublicKey`
                      no perfil do barbeiro no user-service.
                    - Redireciona o barbeiro para `/barberHome?mpLinked=true`.
                    
                    **Esta rota é pública** — chamada diretamente pelo Mercado Pago.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirecionamento para o dashboard do barbeiro após sucesso"),
            @ApiResponse(responseCode = "400", description = "Código inválido ou ausente",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro ao trocar code por token no MP",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @SecurityRequirements
    @GetMapping("/mp-callback")
    public ResponseEntity<Void> handleMpCallback(
            @Parameter(description = "Código de autorização retornado pelo Mercado Pago")
            @RequestParam("code") String code,
            @Parameter(description = "UUID do barbeiro, passado no parâmetro state durante o mp-connect")
            @RequestParam("state") String state) {

        try {
            UUID barberId = UUID.fromString(state);

            if (!isOAuthFlowEnabled()) {
                log.warn("event=mp-oauth-callback-skipped barberId={} reason=missing_client_secret", maskIdentifier(barberId));
                return ResponseEntity.status(302)
                        .location(URI.create(buildPostConnectRedirectUrl(false, "oauth_disabled_in_test")))
                        .build();
            }

            // Troca o authorization code pelo access_token
            String formBody = "grant_type=authorization_code" +
                    "&client_id=" + URLEncoder.encode(mpClientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(mpClientSecret, StandardCharsets.UTF_8) +
                    "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(mpRedirectUri, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mpApiBaseUrl + "/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String error = root.path("error_description").asText("Erro desconhecido");
                                log.error("event=mp-oauth-token-exchange-failed status={} message={}", response.statusCode(), sanitizeMessage(error));
                throw new RuntimeException("Falha ao trocar code pelo access_token: " + error);
            }

            String accessToken = root.path("access_token").asText();
            String refreshToken = root.path("refresh_token").asText(null);
            String mpUserId = root.path("user_id").asText();
            String publicKey = root.path("public_key").asText(null);

            // Salva credenciais no user-service via Feign
            userServiceClient.saveMpCredentials(barberId, new SaveMpCredentialsDTO(
                    accessToken, refreshToken, mpUserId, publicKey
            ));

            log.info("event=mp-oauth-linked barberId={} mpUserId={}", maskIdentifier(barberId), maskIdentifier(mpUserId));

            // Redireciona para o dashboard do barbeiro
            return ResponseEntity.status(302)
                    .location(URI.create(mpPostConnectRedirectUrl))
                    .build();

        } catch (IllegalArgumentException e) {
                        log.error("event=mp-oauth-invalid-state state={}", maskIdentifier(state));
            throw new RuntimeException("Parâmetro state inválido.");
        } catch (Exception e) {
                        log.error("event=mp-oauth-callback-error cause={}", e.getClass().getSimpleName());
            throw new RuntimeException("Erro ao processar callback do Mercado Pago: " + e.getMessage());
        }
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

        private String sanitizeMessage(String value) {
                if (value == null || value.isBlank()) {
                        return "n/a";
                }

                return value
                                .replaceAll("(?i)token[=:\\s]+[^\\s,;]+", "token=***")
                                .replaceAll("(?i)secret[=:\\s]+[^\\s,;]+", "secret=***");
        }

        private boolean isOAuthFlowEnabled() {
                return mpClientSecret != null && !mpClientSecret.isBlank();
        }

        private String buildPostConnectRedirectUrl(boolean linked, String reason) {
                String redirectBase = mpPostConnectRedirectUrl;
                int queryIndex = redirectBase.indexOf('?');
                if (queryIndex >= 0) {
                        redirectBase = redirectBase.substring(0, queryIndex);
                }

                String separator = redirectBase.contains("?") ? "&" : "?";
                String linkedValue = linked ? "true" : "false";
                return redirectBase
                                + separator
                                + "mpLinked=" + linkedValue
                                + "&mpReason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        }
}
