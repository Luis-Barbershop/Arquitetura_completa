package ifsp.edu.projeto.cortaai.apigateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String allowedOriginsRaw;

    @Bean
    @Order(-200)
    public WebFilter corsFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String origin = exchange.getRequest().getHeaders().getFirst(HttpHeaders.ORIGIN);

            if (origin == null || origin.isBlank()) {
                return chain.filter(exchange);
            }

            List<String> allowedOrigins = Arrays.asList(allowedOriginsRaw.split(","));
            boolean originAllowed = allowedOrigins.stream()
                    .map(String::trim)
                    .anyMatch(allowed -> {
                        if (allowed.contains("*")) {
                            String regex = allowed.replace(".", "\\.").replace("*", ".*");
                            return origin.matches(regex);
                        }
                        return allowed.equals(origin);
                    });

            if (!originAllowed) {
                // 👇 LOG ADICIONADO PARA DESCOBRIR A URL QUE ESTÁ SENDO BLOQUEADA 👇
                log.warn("🚫 CORS BLOQUEADO! O navegador enviou a Origem: '{}'. Origens permitidas são: {}", origin, allowedOriginsRaw);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            log.debug("✅ CORS PERMITIDO para a origem: '{}'", origin);

            HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
            responseHeaders.set("Access-Control-Allow-Origin", origin);
            responseHeaders.set("Access-Control-Allow-Credentials", "true");
            responseHeaders.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
            responseHeaders.set("Access-Control-Allow-Headers",
                    "Authorization, Content-Type, Accept, Origin, X-Requested-With, X-User-UID, X-User-Email, X-User-Type, X-User-Name, Cache-Control");
            responseHeaders.set("Access-Control-Expose-Headers",
                    "Authorization, X-User-UID, X-User-Email, X-User-Type, Content-Disposition");
            responseHeaders.set("Access-Control-Max-Age", "600");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
                exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }
}