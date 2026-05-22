package ifsp.edu.projeto.cortaai.apigateway.filter;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseTokenGatewayFilterTest {

    @Test
    void shouldPassOptionsRequestsWithoutAuthentication() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/appointments").build());
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        }).block();

        assertThat(chainedExchange.get()).isNotNull();
        assertThat(chainedExchange.get().getRequest().getHeaders().getFirst("X-Correlation-Id"))
                .isNotBlank();
    }

    @Test
    void shouldStripIdentityAndAuthorizationHeadersFromPublicRoutes() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments/webhook")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer client-token")
                        .header("X-User-UID", "spoofed-user")
                        .header("X-User-Email", "spoofed@cortaai.com")
                        .header("X-User-Type", "OWNER")
                        .build());
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        }).block();

        HttpHeaders headers = chainedExchange.get().getRequest().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(headers.getFirst("X-User-UID")).isNull();
        assertThat(headers.getFirst("X-User-Email")).isNull();
        assertThat(headers.getFirst("X-User-Type")).isNull();
    }

    @Test
    void shouldKeepAuthorizationOnlyForExplicitPublicAuthRoutes() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer client-token")
                        .header("X-User-UID", "spoofed-user")
                        .build());
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        }).block();

        HttpHeaders headers = chainedExchange.get().getRequest().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer client-token");
        assertThat(headers.getFirst("X-User-UID")).isNull();
    }

    @Test
    void shouldRejectPrivateRoutesWhenTokenIsMissing() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header("X-Correlation-Id", "cid-test-001")
                        .build());
        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);

        filter.filter(exchange, chained -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id"))
                .isEqualTo("cid-test-001");
    }

    @Test
    void shouldRequireCookieWhenCookieModeDisablesBearerFallback() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null);
        ReflectionTestUtils.setField(filter, "sessionCookieEnabled", true);
        ReflectionTestUtils.setField(filter, "sessionBearerFallbackEnabled", false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ignored-token")
                        .build());

        filter.filter(exchange, chained -> Mono.error(new AssertionError("chain should not be called"))).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldAuthenticatePrivateRouteAndInjectIdentityHeaders() throws Exception {
        AtomicReference<String> verifiedToken = new AtomicReference<>();
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null, token -> {
            verifiedToken.set(token);
            return firebaseToken(Map.of(
                    "sub", "firebase-uid-123",
                    "email", "ana@cortaai.com",
                    "role", "OWNER",
                    "isOwner", "true",
                    "email_verified", true,
                    "firebase", Map.of("sign_in_provider", "password")
            ));
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .header("X-Correlation-Id", "cid-success")
                        .header("X-User-UID", "spoofed-user")
                        .header("X-User-Type", "CUSTOMER")
                        .build());
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        }).block();

        HttpHeaders headers = chainedExchange.get().getRequest().getHeaders();
        assertThat(verifiedToken.get()).isEqualTo("valid-token");
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(headers.getFirst("X-Correlation-Id")).isEqualTo("cid-success");
        assertThat(headers.getFirst("X-User-UID")).isEqualTo("firebase-uid-123");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("ana@cortaai.com");
        assertThat(headers.getFirst("X-User-Type")).isEqualTo("OWNER");
        assertThat(headers.getFirst("X-User-Owner")).isEqualTo("true");
    }

    @Test
    void shouldRejectPasswordProviderWhenEmailIsNotVerified() throws Exception {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null, token -> firebaseToken(Map.of(
                "sub", "firebase-uid-123",
                "email", "ana@cortaai.com",
                "email_verified", false,
                "firebase", Map.of("sign_in_provider", "password")
        )));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                        .build());
        AtomicReference<Boolean> chainCalled = new AtomicReference<>(false);

        filter.filter(exchange, chained -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertThat(chainCalled.get()).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("E-mail ainda nao verificado.");
    }

    @Test
    void shouldAllowFederatedProviderWithoutVerifiedEmailClaim() throws Exception {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null, token -> firebaseToken(Map.of(
                "sub", "google-uid-123",
                "email", "google@cortaai.com",
                "firebase", Map.of("sign_in_provider", "google.com")
        )));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer google-token")
                        .build());
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        }).block();

        assertThat(chainedExchange.get().getRequest().getHeaders().getFirst("X-User-UID"))
                .isEqualTo("google-uid-123");
        assertThat(chainedExchange.get().getRequest().getHeaders().getFirst("X-User-Type"))
                .isEqualTo("CUSTOMER");
    }

    @Test
    void shouldRejectInvalidFirebaseToken() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null, token -> {
            throw new FirebaseAuthException(new FirebaseException(
                    ErrorCode.UNAUTHENTICATED,
                    "Authorization token=secret-token expired",
                    null
            ));
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
                        .build());

        filter.filter(exchange, chained -> Mono.error(new AssertionError("chain should not be called"))).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains("Token inválido ou expirado.");
    }

    @Test
    void shouldAcceptSseQueryTokenForNotificationStream() throws Exception {
        AtomicReference<String> verifiedToken = new AtomicReference<>();
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null, token -> {
            verifiedToken.set(token);
            return firebaseToken(Map.of("sub", "stream-user", "email_verified", true));
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/notifications/stream?token=query-token").build());
        AtomicReference<ServerWebExchange> chainedExchange = new AtomicReference<>();

        filter.filter(exchange, chained -> {
            chainedExchange.set(chained);
            return Mono.empty();
        }).block();

        assertThat(verifiedToken.get()).isEqualTo("query-token");
        assertThat(chainedExchange.get().getRequest().getHeaders().getFirst("X-User-UID")).isEqualTo("stream-user");
    }

    @Test
    void shouldPreferSessionCookieTokenWhenCookieModeIsEnabled() throws Exception {
        AtomicReference<String> verifiedToken = new AtomicReference<>();
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null, token -> {
            verifiedToken.set(token);
            return firebaseToken(Map.of("sub", "cookie-user", "email_verified", true));
        });
        ReflectionTestUtils.setField(filter, "sessionCookieEnabled", true);
        ReflectionTestUtils.setField(filter, "sessionCookieName", "cortaai_session");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/appointments")
                        .cookie(new HttpCookie("cortaai_session", "cookie-token"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bearer-token")
                        .build());

        filter.filter(exchange, chained -> Mono.empty()).block();

        assertThat(verifiedToken.get()).isEqualTo("cookie-token");
    }

    @Test
    void shouldTreatPublicGetBarberRoutesAsPublicButPrivateBarbershopRoutesAsProtected() {
        FirebaseTokenGatewayFilter filter = new FirebaseTokenGatewayFilter(null);
        MockServerWebExchange publicExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/barbers/123")
                        .header("X-User-UID", "spoofed")
                        .build());
        AtomicReference<ServerWebExchange> publicChainedExchange = new AtomicReference<>();

        filter.filter(publicExchange, chained -> {
            publicChainedExchange.set(chained);
            return Mono.empty();
        }).block();

        MockServerWebExchange privateExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/barbershops/my-shop/123").build());

        filter.filter(privateExchange, chained -> Mono.error(new AssertionError("chain should not be called"))).block();

        assertThat(publicChainedExchange.get().getRequest().getHeaders().getFirst("X-User-UID")).isNull();
        assertThat(privateExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldExposeGatewayFilterOrder() {
        assertThat(new FirebaseTokenGatewayFilter(null).getOrder()).isEqualTo(-100);
    }

    private static FirebaseToken firebaseToken(Map<String, Object> overrides) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "https://securetoken.google.com/test-project");
        claims.put("aud", "test-project");
        claims.put("sub", "default-uid");
        claims.put("iat", Instant.now().minusSeconds(60).getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
        claims.putAll(overrides);

        try {
            Constructor<FirebaseToken> constructor = FirebaseToken.class.getDeclaredConstructor(Map.class);
            constructor.setAccessible(true);
            return constructor.newInstance(claims);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to create test FirebaseToken", ex);
        }
    }
}
