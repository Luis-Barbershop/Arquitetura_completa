package ifsp.edu.projeto.cortaai.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class GatewayRateLimitConfig {

    @Bean
    public KeyResolver userOrIpRateLimitKeyResolver() {
        return exchange -> Mono.just(resolveKey(exchange));
    }

    @Bean
    public KeyResolver authUserOrIpRateLimitKeyResolver() {
        return exchange -> Mono.just("auth:" + resolveKey(exchange));
    }

    private String resolveKey(ServerWebExchange exchange) {
        String userId = firstHeader(exchange, "X-User-UID");
        if (hasText(userId)) {
            return "user:" + userId.trim();
        }

        return "ip:" + resolveClientIp(exchange.getRequest());
    }

    private String resolveClientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (hasText(realIp)) {
            return realIp.trim();
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "unknown";
    }

    private String firstHeader(ServerWebExchange exchange, String headerName) {
        return exchange.getRequest().getHeaders().getFirst(headerName);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
