package ifsp.edu.projeto.cortaai.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração centralizada de CORS para toda a aplicação.
 *
 * <p>Este bean {@link CorsWebFilter} é registrado no contexto reativo do Gateway
 * e intercepta <b>todos</b> os requests (inclusive preflight OPTIONS) antes de
 * qualquer filtro de roteamento. Isso garante que:
 * <ul>
 *   <li>Requisições preflight (OPTIONS) recebam os headers corretos e retornem 200</li>
 *   <li>O header {@code Access-Control-Allow-Origin} reflita a origin da requisição</li>
 *   <li>Credenciais (Bearer token) sejam permitidas</li>
 * </ul>
 *
 * <p>As origens permitidas são configuráveis via variável de ambiente
 * {@code CORS_ALLOWED_ORIGINS} (separadas por vírgula). Em desenvolvimento,
 * o padrão permite {@code http://localhost:*}.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String allowedOriginsRaw;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ── Origens permitidas ──────────────────────────────────────────
        // Suporta patterns com wildcard (ex: http://localhost:*)
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        for (String origin : origins) {
            String trimmed = origin.trim();
            if (trimmed.contains("*")) {
                config.addAllowedOriginPattern(trimmed);
            } else {
                config.addAllowedOrigin(trimmed);
            }
        }

        // ── Métodos HTTP permitidos ─────────────────────────────────────
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        // ── Headers permitidos na requisição ────────────────────────────
        config.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-User-UID",
                "X-User-Email",
                "X-User-Type",
                "X-User-Name",
                "Cache-Control"
        ));

        // ── Headers expostos ao navegador na resposta ───────────────────
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-User-UID",
                "X-User-Email",
                "X-User-Type",
                "Content-Disposition"
        ));

        // ── Permitir credenciais (Bearer token, cookies) ────────────────
        config.setAllowCredentials(true);

        // ── Cache da resposta preflight (10 min) ────────────────────────
        config.setMaxAge(600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
