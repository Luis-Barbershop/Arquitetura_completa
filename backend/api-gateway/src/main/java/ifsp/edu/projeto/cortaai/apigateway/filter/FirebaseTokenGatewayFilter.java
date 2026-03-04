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

/**
 * Filtro global do API Gateway.
 *
 * <p>Valida o Firebase ID Token (Bearer) presente no header {@code Authorization}.
 * Em rotas públicas, a requisição é deixada passar sem validação.
 * Em rotas protegidas, caso o token seja inválido ou ausente, retorna HTTP 401.
 *
 * <p>Após validação bem-sucedida, injeta os seguintes headers para os serviços downstream:
 * <ul>
 *   <li>{@code X-User-UID}   — UID único do Firebase</li>
 *   <li>{@code X-User-Email} — e-mail do usuário</li>
 *   <li>{@code X-User-Name}  — nome de exibição</li>
 *   <li>{@code X-User-Type}  — hint do tipo (CUSTOMER | BARBER), enviado pelo cliente como query param ou header</li>
 * </ul>
 */
@Component
public class FirebaseTokenGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenGatewayFilter.class);

    /** Rotas que NÃO exigem autenticação Firebase. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/verify",          // login/registro via Firebase token
            "/api/auth/social/**",        // compatibilidade futura
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/**",
            "/api/barbers",              // listagem pública de barbeiros
            "/api/barbers/**"            // detalhes públicos de barbeiro
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
        String path = exchange.getRequest().getURI().getPath();

        // Rotas públicas — deixa passar sem validação
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extrai o token do header Authorization: Bearer <token>
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "Token de autenticação ausente ou malformado.");
        }

        String idToken = authHeader.substring(7);

        // Validação do token é bloqueante — executamos em thread do pool boundedElastic
        return Mono.fromCallable(() -> firebaseAuth.verifyIdToken(idToken))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(decodedToken -> {
                    ServerHttpRequest mutatedRequest = buildMutatedRequest(exchange, decodedToken);
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(FirebaseAuthException.class, ex -> {
                    log.warn("Token Firebase inválido ou expirado: {}", ex.getMessage());
                    return unauthorized(exchange, "Token inválido ou expirado.");
                })
                .onErrorResume(Exception.class, ex -> {
                    log.error("Erro ao validar token Firebase", ex);
                    return unauthorized(exchange, "Erro de autenticação.");
                });
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * Constrói a requisição mutada com headers de identidade injetados.
     * Headers anteriores de identidade (X-User-*) são removidos para evitar spoofing.
     */
    private ServerHttpRequest buildMutatedRequest(ServerWebExchange exchange, FirebaseToken token) {
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
                })
                .header("X-User-UID", token.getUid())
                .header("X-User-Email", token.getEmail() != null ? token.getEmail() : "")
                .header("X-User-Name",  token.getName()  != null ? token.getName()  : "")
                .header("X-User-Type",  userType)
                .build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
