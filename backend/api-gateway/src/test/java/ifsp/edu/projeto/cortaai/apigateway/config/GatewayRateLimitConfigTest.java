package ifsp.edu.projeto.cortaai.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRateLimitConfigTest {

    private final GatewayRateLimitConfig config = new GatewayRateLimitConfig();

    @Test
    void shouldPreferAuthenticatedUserIdAsRateLimitKey() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header("X-User-UID", "firebase-user-123")
                        .remoteAddress(new InetSocketAddress("10.0.0.10", 12345))
                        .build()
        );

        String key = config.userOrIpRateLimitKeyResolver().resolve(exchange).block();

        assertThat(key).isEqualTo("user:firebase-user-123");
    }

    @Test
    void shouldUseFirstForwardedIpWhenUserIdIsMissing() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/barbershops")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.5")
                        .remoteAddress(new InetSocketAddress("10.0.0.10", 12345))
                        .build()
        );

        String key = config.userOrIpRateLimitKeyResolver().resolve(exchange).block();

        assertThat(key).isEqualTo("ip:203.0.113.10");
    }

    @Test
    void shouldUseDedicatedAuthBucketPrefix() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/email/login")
                        .header("X-Forwarded-For", "203.0.113.20")
                        .build()
        );

        String key = config.authUserOrIpRateLimitKeyResolver().resolve(exchange).block();

        assertThat(key).isEqualTo("auth:ip:203.0.113.20");
    }
}
