package ifsp.edu.projeto.cortaai.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Configuração centralizada de CORS para toda a aplicação.
 *
 * <p>Intercepta todos os requests e aplica CORS apenas quando o header
 * {@code Origin} está presente. Requests sem Origin (ex: proxy interno,
 * curl, serviço a serviço) passam sem verificação CORS.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String allowedOriginsRaw;

    @Bean
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);

            // Se não tem header Origin, NÃO é request CORS — deixa passar
            if (origin == null || origin.isBlank()) {
                return chain.filter(exchange);
            }

            // Verifica se a origin é permitida
            List<String> allowedOrigins = Arrays.asList(allowedOriginsRaw.split(","));
            boolean originAllowed = allowedOrigins.stream()
                    .map(String::trim)
                    .anyMatch(allowed -> {
                        if (allowed.contains("*")) {
                            // Pattern match simples: http://localhost:* → http://localhost:XXXX
                            String regex = allowed.replace(".", "\\.").replace("*", ".*");
                            return origin.matches(regex);
                        }
                        return allowed.equals(origin);
                    });

            if (!originAllowed) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            // Adiciona headers CORS na resposta
            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
            responseHeaders.set("Access-Control-Allow-Origin", origin);
            responseHeaders.set("Access-Control-Allow-Credentials", "true");
            responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
            responseHeaders.set("Access-Control-Allow-Headers",
                    "Authorization, Content-Type, Accept, Origin, X-Requested-With, X-User-UID, X-User-Email, X-User-Type, X-User-Name, Cache-Control");
            responseHeaders.set("Access-Control-Expose-Headers",
                    "Authorization, X-User-UID, X-User-Email, X-User-Type, Content-Disposition");
            responseHeaders.set("Access-Control-Max-Age", "600");

            // Preflight OPTIONS — responde direto sem encaminhar
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.NO_CONTENT);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }
}
