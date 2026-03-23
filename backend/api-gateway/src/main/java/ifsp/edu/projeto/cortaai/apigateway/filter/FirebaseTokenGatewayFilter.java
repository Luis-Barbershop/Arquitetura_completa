package ifsp.edu.projeto.cortaai.apigateway.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
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

import java.util.List;
import java.util.UUID;

/**
 * Filtro global do API Gateway.
 *
 * <p>Valida o Firebase ID Token (Bearer) presente no header {@code Authorization}.
 * Em rotas públicas, a requisição é deixada passar sem validação.
 * Em rotas protegidas, caso o token seja inválido ou ausente, retorna HTTP 401.
 *
 * <p>Após validação bem-sucedida, injeta os seguintes headers para os serviços downstream:
 * <ul>
 * <li>{@code X-User-UID}   — UID único do Firebase</li>
 * <li>{@code X-User-Email} — e-mail do usuário</li>
 * <li>{@code X-User-Name}  — nome de exibição</li>
 * <li>{@code X-User-Type}  — hint do tipo (CUSTOMER | BARBER), enviado pelo cliente como query param ou header</li>
 * </ul>
 */
@Component
public class FirebaseTokenGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenGatewayFilter.class);

    /** Rotas que NÃO exigem autenticação Firebase. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/verify", "/api/auth/verify/",
            "/api/auth/firebase-test/sign-in-email", "/api/auth/firebase-test/sign-in-email/",
            "/api/auth/firebase-test/verify-id-token", "/api/auth/firebase-test/verify-id-token/",
            "/api/auth/firebase-test/register-email", "/api/auth/firebase-test/register-email/",
            "/api/customers/login", "/api/customers/login/",
            "/api/barbers/login", "/api/barbers/login/",
            "/api/customers/register", "/api/customers/register/",
            "/api/barbers/register", "/api/barbers/register/",
            "/api/auth/social/**",        // compatibilidade futura
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/**",
            "/api/barbers",              // listagem pública de barbeiros
            "/api/barbers/**",           // detalhes públicos de barbeiro
            "/api/payments/webhook",     // webhook do Mercado Pago (sem autenticação)
            "/api/payments/webhook/",
            "/api/internal/**"           // endpoints internos (Feign inter-serviço)
    );

    /** Endpoints públicos de leitura de barbearia (somente GET). */
    private static final List<String> PUBLIC_BARBERSHOP_GET_PATHS = List.of(
            "/api/barbershops",
            "/api/barbershops/ping",
            "/api/barbershops/*",
            "/api/barbershops/*/activities"
    );

    private final FirebaseAuth firebaseAuth;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public FirebaseTokenGatewayFilter(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public int getOrder() {
        // -100 garante que este filtro rode antes dos filtros de roteamento
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest().getHeaders().getFirst("X-Correlation-Id"));
        ServerWebExchange exchangeWithCorrelation = withCorrelationId(exchange, correlationId);

        String method = exchangeWithCorrelation.getRequest().getMethod().name();

        // Preflight CORS (OPTIONS) — NUNCA bloquear; o CorsConfig cuida disso
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchangeWithCorrelation);
        }

        String path = exchangeWithCorrelation.getRequest().getURI().getPath();

        // Rotas públicas — deixa passar sem validação
        if (isPublicPath(path) || isPublicBarbershopGet(path, method)) {
            return chain.filter(exchangeWithCorrelation);
        }

        // Extrai o token do header Authorization: Bearer <token>
        String authHeader = exchangeWithCorrelation.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchangeWithCorrelation, "Token de autenticação ausente ou malformado.", correlationId);
        }

        String idToken = authHeader.substring(7);

        // Validação do token é bloqueante — executamos em thread do pool boundedElastic
        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(decodedToken -> {
                    ServerHttpRequest mutatedRequest = buildMutatedRequest(exchangeWithCorrelation, decodedToken, correlationId);
                    return chain.filter(exchangeWithCorrelation.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(FirebaseAuthException.class, ex -> {
                    log.warn("event=gateway-firebase-invalid-token correlationId={} message={}", correlationId, ex.getMessage());
                    return unauthorized(exchangeWithCorrelation, "Token inválido ou expirado.", correlationId);
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("event=gateway-firebase-auth-error correlationId={} message={}", correlationId, ex.getMessage(), ex);
                    return unauthorized(exchangeWithCorrelation, "Erro de autenticação.", correlationId);
                });
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isPublicBarbershopGet(String path, String method) {
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        return PUBLIC_BARBERSHOP_GET_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Constrói a requisição mutada com headers de identidade injetados.
     * Headers anteriores de identidade (X-User-*) são removidos para evitar spoofing.
     */
    private ServerHttpRequest buildMutatedRequest(ServerWebExchange exchange, FirebaseToken token, String correlationId) {
        // Tenta obter o tipo do usuário a partir do header enviado pelo cliente
        String userType = exchange.getRequest().getHeaders().getFirst("X-User-Type");
        if (userType == null) {
            userType = "UNKNOWN";
        }

        return exchange.getRequest().mutate()
                // Remove headers que o cliente poderia ter injetado (anti-spoofing)
                .headers(headers -> {
                    headers.remove("X-User-UID");
                    headers.remove("X-User-Email");
                    headers.remove("X-User-Name");
                    headers.remove("X-User-Type");
                    headers.remove("X-Correlation-Id");
                })
                .header("X-User-UID", token.getUid())
                .header("X-User-Email", token.getEmail() != null ? token.getEmail() : "")
                .header("X-User-Name",  token.getName()  != null ? token.getName()  : "")
                .header("X-User-Type",  userType)
                .header("X-Correlation-Id", correlationId)
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message, String correlationId) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"correlationId\":\"" + correlationId + "\"}";
        var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
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