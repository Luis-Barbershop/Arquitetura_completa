package ifsp.edu.projeto.cortaai.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Filtro global do API Gateway.
 *
 * <p>Valida o Firebase ID Token (Bearer) presente no header {@code Authorization}.
 * Em rotas públicas, a requisição é deixada passar sem validação.
 * Em rotas protegidas, caso o token seja inválido ou ausente, retorna HTTP 401.
 */
@Component
public class FirebaseTokenGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenGatewayFilter.class);

    @Value("${session.cookie.enabled:false}")
    private boolean sessionCookieEnabled;

    @Value("${session.bearer-fallback.enabled:true}")
    private boolean sessionBearerFallbackEnabled;

    @Value("${session.cookie.name:cortaai_session}")
    private String sessionCookieName;

    /** Rotas que NÃO exigem autenticação Firebase. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/verify", "/api/auth/verify/",
            "/api/auth/email/login", "/api/auth/email/login/",
            "/api/auth/email/verify-token", "/api/auth/email/verify-token/",
            "/api/auth/email/register", "/api/auth/email/register/",
            // Verificação de e-mail cadastrado — usada no redirect inteligente do login
            "/api/auth/email/exists", "/api/auth/email/exists/",
            // Recuperação e alteração de senha — não exigem token de Authorization
            "/api/auth/email/forgot-password", "/api/auth/email/forgot-password/",
            "/api/auth/email/change-password", "/api/auth/email/change-password/",
            // Reenvio de e-mail de verificação — usuário não está logado
            "/api/auth/email/resend-verification", "/api/auth/email/resend-verification/",
            "/api/auth/firebase-test/resend-verification", "/api/auth/firebase-test/resend-verification/",
            "/api/auth/firebase-test/forgot-password", "/api/auth/firebase-test/forgot-password/",
            "/api/auth/firebase-test/sign-in-email", "/api/auth/firebase-test/sign-in-email/",
            "/api/auth/firebase-test/verify-id-token", "/api/auth/firebase-test/verify-id-token/",
            "/api/auth/firebase-test/register-email", "/api/auth/firebase-test/register-email/",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/**",
            "/api/payments/webhook", "/api/payments/webhook/",
            "/api/internal/**"
    );

    /**
     * Endpoints públicos que podem receber Authorization do cliente
     * por necessidade explícita do fluxo de autenticação/validação.
     */
    private static final List<String> PUBLIC_AUTHORIZATION_ALLOWED_PATHS = List.of(
        "/api/auth/verify", "/api/auth/verify/",
        "/api/auth/firebase-test/verify-id-token", "/api/auth/firebase-test/verify-id-token/"
    );

    /** Endpoints públicos de leitura de barbeiros (somente GET). */
    private static final List<String> PUBLIC_BARBERS_GET_PATHS = List.of(
            "/api/barbers",
            "/api/barbers/*",
            "/api/barbers/barbershop/*",
            "/api/barbers/*/activities"
    );

    /** Endpoints públicos de leitura de barbearia (somente GET). */
    private static final List<String> PUBLIC_BARBERSHOP_GET_PATHS = List.of(
            "/api/barbershops",
            "/api/barbershops/*",
            "/api/barbershops/*/activities",
            "/api/barbershops/*/barbers"  // lista de barbeiros da barbearia (rota pública)
    );

        /** Endpoints privados de barbearia que nunca devem ser tratados como públicos. */
        private static final List<String> PRIVATE_BARBERSHOP_PATHS = List.of(
            "/api/barbershops/my-invites",
            "/api/barbershops/my-shop/**",
            "/api/barbershops/accept-invite/**",
            "/api/barbershops/reject-invite/**",
            "/api/barbershops/join-request",
            "/api/barbershops/leave-shop"
        );

    private final FirebaseAuth firebaseAuth;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public FirebaseTokenGatewayFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));
        ServerWebExchange exchangeWithCorrelation = withCorrelationId(exchange, correlationId);

        String method = exchangeWithCorrelation.getRequest().getMethod().name();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchangeWithCorrelation);
        }

        String path = exchangeWithCorrelation.getRequest().getURI().getPath();
        if (isPublicPath(path) || isPublicGetPath(path, method)) {
            ServerHttpRequest sanitizedPublicRequest = sanitizePublicRequest(exchangeWithCorrelation, path);
            return chain.filter(exchangeWithCorrelation.mutate().request(sanitizedPublicRequest).build());
        }

        TokenResolution tokenResolution = resolveToken(exchangeWithCorrelation, correlationId, path);
        if (tokenResolution.token() == null || tokenResolution.token().isBlank()) {
            return unauthorized(
                    exchangeWithCorrelation,
                    "Token de autenticação ausente ou malformado.",
                    correlationId,
                    "auth_token_missing",
                    tokenResolution.source()
            );
        }

        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(tokenResolution.token()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(decodedToken -> {
                    if (requiresVerifiedEmail(decodedToken)) {
                        log.info("event=gateway-email-not-verified uid={} correlationId={}", maskIdentifier(decodedToken.getUid()), correlationId);
                        return unauthorized(
                                exchangeWithCorrelation,
                                "E-mail ainda nao verificado.",
                                correlationId,
                                "email_not_verified",
                                tokenResolution.source()
                        );
                    }

                    log.info(
                            "event=session-auth-success correlationId={} path={} authSource={} uid={} role={}",
                            correlationId,
                            path,
                            tokenResolution.source(),
                            maskIdentifier(decodedToken.getUid()),
                            decodedToken.getClaims().getOrDefault("role", "CUSTOMER")
                    );

                    ServerHttpRequest mutatedRequest = buildMutatedRequest(exchangeWithCorrelation, decodedToken, correlationId);
                    return chain.filter(exchangeWithCorrelation.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(FirebaseAuthException.class, ex -> {
                    log.warn(
                            "event=session-auth-invalid-token correlationId={} path={} authSource={} message={}",
                            correlationId,
                            path,
                            tokenResolution.source(),
                            sanitizeExceptionMessage(ex)
                    );
                    return unauthorized(
                            exchangeWithCorrelation,
                            "Token inválido ou expirado.",
                            correlationId,
                            "invalid_or_expired_token",
                            tokenResolution.source()
                    );
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error(
                            "event=session-auth-processing-error correlationId={} path={} authSource={} message={}",
                            correlationId,
                            path,
                            tokenResolution.source(),
                            sanitizeExceptionMessage(ex),
                            ex
                    );
                    return unauthorized(
                            exchangeWithCorrelation,
                            "Erro de autenticação.",
                            correlationId,
                            "auth_processing_error",
                            tokenResolution.source()
                    );
                });
    }

    private TokenResolution resolveToken(ServerWebExchange exchange, String correlationId, String path) {
        String cookieToken = extractCookieToken(exchange);
        if (sessionCookieEnabled && cookieToken != null && !cookieToken.isBlank()) {
            log.info("event=session-cookie-token-used correlationId={} path={}", correlationId, path);
            return new TokenResolution(cookieToken, "COOKIE", "cookie_token_present");
        }

        String bearerToken = extractBearerToken(exchange);
        if (bearerToken != null && !bearerToken.isBlank()) {
            if (sessionCookieEnabled) {
                log.info("event=session-cookie-token-fallback-bearer correlationId={} path={}", correlationId, path);
            }
            return new TokenResolution(bearerToken, "BEARER", "bearer_token_present");
        }

        if (sessionCookieEnabled && !sessionBearerFallbackEnabled) {
            log.warn("event=session-cookie-auth-missing correlationId={} path={} cookieName={}", correlationId, path, sessionCookieName);
            return new TokenResolution(null, "NONE", "cookie_required_missing");
        }

        return new TokenResolution(null, "NONE", "bearer_missing_or_malformed");
    }

    private record TokenResolution(String token, String source, String reason) {
    }

    private String extractCookieToken(ServerWebExchange exchange) {
        if (!sessionCookieEnabled || sessionCookieName == null || sessionCookieName.isBlank()) {
            return null;
        }

        var cookie = exchange.getRequest().getCookies().getFirst(sessionCookieName);
        if (cookie == null) {
            return null;
        }

        return cookie.getValue();
    }

    private String extractBearerToken(ServerWebExchange exchange) {
        if (sessionCookieEnabled && !sessionBearerFallbackEnabled) {
            return null;
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }

    private String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "***";
        }

        String normalized = value.trim();
        if (normalized.length() <= 6) {
            return "***";
        }

        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 2);
    }

    private String sanitizeExceptionMessage(Throwable ex) {
        String message = ex != null ? ex.getMessage() : null;
        if (message == null || message.isBlank()) {
            return "n/a";
        }

        return message
                .replaceAll("(?i)bearer\\s+[a-z0-9._-]+", "bearer ***")
                .replaceAll("(?i)authorization[^\\s]*", "authorization***")
                .replaceAll("(?i)token[=:\\s]+[^\\s,;]+", "token=***");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isPublicGetPath(String path, String method) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }

        if (isPrivateBarbershopPath(path)) {
            return false;
        }

        return PUBLIC_BARBERS_GET_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path))
                || PUBLIC_BARBERSHOP_GET_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));
    }

    private boolean isPrivateBarbershopPath(String path) {
        return PRIVATE_BARBERSHOP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean requiresVerifiedEmail(FirebaseToken token) {
        String provider = extractSignInProvider(token);
        if (!"password".equalsIgnoreCase(provider)) {
            return false;
        }

        Object emailVerifiedClaim = token.getClaims().get("email_verified");
        if (emailVerifiedClaim instanceof Boolean boolClaim) {
            return !boolClaim;
        }
        if (emailVerifiedClaim instanceof String strClaim) {
            return !Boolean.parseBoolean(strClaim);
        }
        return true;
    }

    private String extractSignInProvider(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            return "";
        }
        Object provider = firebaseMap.get("sign_in_provider");
        return provider != null ? provider.toString() : "";
    }

    /**
     * Constrói a requisição mutada com headers de identidade injetados.
     * Remove headers X-User-* de entrada para evitar spoofing do cliente.
     */
    private ServerHttpRequest buildMutatedRequest(ServerWebExchange exchange, FirebaseToken decodedToken, String correlationId) {
        Map<String, Object> claims = decodedToken.getClaims();

        String role = (String) claims.getOrDefault("role", "CUSTOMER");
        boolean isOwner = false;
        if (claims.containsKey("isOwner")) {
            Object isOwnerClaim = claims.get("isOwner");
            if (isOwnerClaim instanceof Boolean boolClaim) {
                isOwner = boolClaim;
            } else if (isOwnerClaim instanceof String strClaim) {
                isOwner = Boolean.parseBoolean(strClaim);
            }
        }

        return exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.remove("X-User-UID");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Type");
                    headers.remove("X-User-Owner");
                    headers.remove("X-Correlation-ID");
                    headers.remove("X-Correlation-Id");
                })
                .header("X-Correlation-Id", correlationId)
                .header("X-User-UID", decodedToken.getUid())
                .header("X-User-Email", decodedToken.getEmail() != null ? decodedToken.getEmail() : "")
                .header("X-User-Type", role)
                .header("X-User-Owner", String.valueOf(isOwner))
                .build();
    }

    /**
     * Sanitiza headers de identidade em rotas públicas para evitar spoofing.
     * Authorization só é encaminhado quando há necessidade explícita em allowlist.
     */
    private ServerHttpRequest sanitizePublicRequest(ServerWebExchange exchange, String path) {
        return exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-UID");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Type");
                    headers.remove("X-User-Owner");

                    if (!isPublicAuthorizationAllowed(path)) {
                        headers.remove(HttpHeaders.AUTHORIZATION);
                    }
                })
                .build();
    }

    private boolean isPublicAuthorizationAllowed(String path) {
        return PUBLIC_AUTHORIZATION_ALLOWED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(
        ServerWebExchange exchange,
        String message,
        String correlationId,
        String reasonCode,
        String authSource
    ) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);

        String path = exchange.getRequest().getURI().getPath();
    log.warn(
        "event=session-auth-unauthorized correlationId={} path={} reason={} authSource={}",
        correlationId,
        path,
        reasonCode,
        authSource
    );

        String body = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\"," +
                        "\"cause\":\"FirebaseAuthException\",\"origin\":\"api-gateway\"," +
                        "\"path\":\"%s\",\"correlationId\":\"%s\"}",
                java.time.Instant.now().toString(),
                message.replace("\"", "\\\""),
                path != null ? path.replace("\"", "\\\"") : "",
                correlationId
        );

        var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private ServerWebExchange withCorrelationId(ServerWebExchange exchange, String correlationId) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-Correlation-Id");
                    headers.add("X-Correlation-Id", correlationId);
                })
                .build();

        ServerWebExchange mutated = exchange.mutate().request(request).build();
        mutated.getAttributes().put("correlationId", correlationId);
        mutated.getResponse().getHeaders().set("X-Correlation-Id", correlationId);
        return mutated;
    }

    private String resolveCorrelationId(String incomingCorrelationId) {
        if (incomingCorrelationId == null || incomingCorrelationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return incomingCorrelationId;
    }
}

