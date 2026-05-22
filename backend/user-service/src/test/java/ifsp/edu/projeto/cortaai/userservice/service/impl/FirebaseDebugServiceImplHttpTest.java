package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.google.firebase.auth.FirebaseToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ForgotPasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ResendVerificationRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirebaseDebugServiceImplHttpTest {

    @Mock
    private FirebaseAuthService firebaseAuthService;
    @Mock
    private BarberRepository barberRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private HttpClient httpClient;

    private FirebaseDebugServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FirebaseDebugServiceImpl(
                null,
                firebaseAuthService,
                new ObjectMapper(),
                barberRepository,
                customerRepository,
                httpClient
        );
        ReflectionTestUtils.setField(service, "firebaseWebApiKey", "web-key");
        ReflectionTestUtils.setField(service, "appWebBaseUrl", "https://app.example.com");
        ReflectionTestUtils.setField(service, "forgotPasswordContinueUrl", "https://app.example.com/change-password");
    }

    @Test
    void shouldSignInWithEmailPassword() throws Exception {
        HttpResponse<String> signInResponse = response(200, """
                {"idToken":"id-token","refreshToken":"refresh","expiresIn":"3600","localId":"uid","email":"ana@example.com","registered":true}
                """);
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(signInResponse);

        var result = service.signInWithEmailPassword(new FirebaseEmailSignInRequestDTO("ana@example.com", "Secret123!"));

        assertThat(result.idToken()).isEqualTo("id-token");
        assertThat(result.registered()).isTrue();
    }

    @Test
    void shouldMapFirebaseSignInErrors() throws Exception {
        HttpResponse<String> signInResponse = response(400, "{\"error\":{\"message\":\"INVALID_PASSWORD\"}}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(signInResponse);

        assertThatThrownBy(() -> service.signInWithEmailPassword(
                new FirebaseEmailSignInRequestDTO("ana@example.com", "wrong")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Senha inválida");
    }

    @Test
    void shouldRegisterBarberAndCompleteProfile() throws Exception {
        AuthResponseDTO profile = new AuthResponseDTO(UUID.randomUUID(), "Bia", "bia@example.com",
                "11999999999", "12345678909", null, "BARBER", "EMAIL", true,
                "ROLE_BARBER", false, true, null, null, true, true);
        HttpResponse<String> signUpResponse = response(200, """
                {"idToken":"id-token","refreshToken":"refresh","expiresIn":"3600","localId":"barber-uid"}
                """);
        HttpResponse<String> verifyEmailResponse = response(200, "{}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(signUpResponse)
                .thenReturn(verifyEmailResponse);
        when(firebaseAuthService.completeBarberProfile(eq("barber-uid"), any(), eq("bia@example.com")))
                .thenReturn(profile);

        var result = service.registerWithEmailPassword(new FirebaseEmailRegisterRequestDTO(
                "bia@example.com",
                "Secret123!",
                "BARBER",
                "Bia",
                "11999999999",
                "12345678909",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                true
        ));

        assertThat(result.localId()).isEqualTo("barber-uid");
        assertThat(result.profile()).isEqualTo(profile);
        verify(firebaseAuthService).verifyAndProvision(new FirebaseAuthRequestDTO("id-token", "BARBER"));
        verify(firebaseAuthService).setCustomUserClaims("barber-uid", "BARBER", true);
    }

    @Test
    void shouldMapRegisterErrorsWithoutProvisioning() throws Exception {
        HttpResponse<String> signUpResponse = response(400, "{\"error\":{\"message\":\"EMAIL_EXISTS\"}}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(signUpResponse);

        assertThatThrownBy(() -> service.registerWithEmailPassword(new FirebaseEmailRegisterRequestDTO(
                "bia@example.com",
                "Secret123!",
                "CUSTOMER",
                "Bia",
                "11999999999",
                "12345678909",
                LocalDate.of(1990, 1, 1),
                null,
                null,
                false
        )))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("já está cadastrado");
    }

    @Test
    void shouldSendForgotPasswordEmailAndApplyResendCooldown() throws Exception {
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        HttpResponse<String> forgotPasswordResponse = response(200, "{}");
        when(httpClient.send(requestCaptor.capture(), anyBodyHandler()))
                .thenReturn(forgotPasswordResponse);

        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(" ANA@Example.COM ");

        service.forgotPassword(request);

        assertThat(requestCaptor.getValue().uri().toString()).contains("sendOobCode?key=web-key");
        assertThatThrownBy(() -> service.resendForgotPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aguarde");
    }

    @Test
    void shouldMapForgotPasswordFirebaseErrors() throws Exception {
        HttpResponse<String> forgotPasswordResponse = response(400, "{\"error\":{\"message\":\"EMAIL_NOT_FOUND\"}}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(forgotPasswordResponse);

        assertThatThrownBy(() -> service.forgotPassword(new ForgotPasswordRequestDTO("missing@example.com")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("E-mail não encontrado");
    }

    @Test
    void shouldResendVerificationEmail() throws Exception {
        HttpResponse<String> signInResponse = response(200, "{\"idToken\":\"fresh-token\"}");
        HttpResponse<String> verifyResponse = response(200, "{}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler()))
                .thenReturn(signInResponse)
                .thenReturn(verifyResponse);

        service.resendVerificationEmail(new ResendVerificationRequestDTO("ana@example.com", "Secret123!"));

        verify(httpClient, org.mockito.Mockito.times(2)).send(any(HttpRequest.class), anyBodyHandler());
    }

    @Test
    void shouldVerifyTokenReturningDebugClaims() throws Exception {
        service = serviceWithToken(firebaseToken("uid", "Ana", "ana@example.com", "password", true));

        var result = service.verifyToken("id-token");

        assertThat(result.uid()).isEqualTo("uid");
        assertThat(result.email()).isEqualTo("ana@example.com");
        assertThat(result.audience()).isEqualTo("cortaai");
        assertThat(result.issuedAt()).isNotNull();
        assertThat(result.expiresAt()).isNotNull();
        assertThat(result.claims()).containsKey("firebase");
    }

    @Test
    void shouldChangePasswordForPasswordProvider() throws Exception {
        service = serviceWithToken(firebaseToken("uid", "Ana", "ana@example.com", "password", true));
        HttpResponse<String> updateResponse = response(200,
                "{\"idToken\":\"new-id-token\",\"refreshToken\":\"new-refresh\",\"localId\":\"uid\"}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenReturn(updateResponse);

        var result = service.changePassword(new ChangePasswordRequestDTO("old-token", "NewSecret123!"));

        assertThat(result.idToken()).isEqualTo("new-id-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void shouldRejectChangePasswordForSocialProviderBeforeHttpCall() throws Exception {
        service = serviceWithToken(firebaseToken("uid", "Ana", "ana@example.com", "google.com", true));

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequestDTO("token", "NewSecret123!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("login social");
    }

    @Test
    void shouldMapChangePasswordFirebaseErrors() throws Exception {
        service = serviceWithToken(firebaseToken("uid", "Ana", "ana@example.com", "password", true));
        HttpResponse<String> updateResponse = response(400, "{\"error\":{\"message\":\"TOKEN_EXPIRED\"}}");
        when(httpClient.send(any(HttpRequest.class), anyBodyHandler())).thenReturn(updateResponse);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequestDTO("old-token", "NewSecret123!")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Sessão expirada");
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int statusCode, String body) {
        HttpResponse<String> response = org.mockito.Mockito.mock(HttpResponse.class);
        org.mockito.Mockito.lenient().when(response.statusCode()).thenReturn(statusCode);
        org.mockito.Mockito.lenient().when(response.body()).thenReturn(body);
        return response;
    }

    private HttpResponse.BodyHandler<String> anyBodyHandler() {
        return org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any();
    }

    private FirebaseDebugServiceImpl serviceWithToken(FirebaseToken token) {
        FirebaseDebugServiceImpl configured = new FirebaseDebugServiceImpl(
                null,
                firebaseAuthService,
                new ObjectMapper(),
                barberRepository,
                customerRepository,
                httpClient,
                idToken -> token
        );
        ReflectionTestUtils.setField(configured, "firebaseWebApiKey", "web-key");
        ReflectionTestUtils.setField(configured, "appWebBaseUrl", "https://app.example.com");
        ReflectionTestUtils.setField(configured, "forgotPasswordContinueUrl", "https://app.example.com/change-password");
        return configured;
    }

    private FirebaseToken firebaseToken(String uid,
                                        String name,
                                        String email,
                                        String provider,
                                        boolean emailVerified) throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", uid);
        claims.put("user_id", uid);
        claims.put("name", name);
        claims.put("email", email);
        claims.put("picture", "https://cdn/avatar.png");
        claims.put("iss", "https://securetoken.google.com/cortaai");
        claims.put("aud", "cortaai");
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plusSeconds(3600).getEpochSecond());
        claims.put("email_verified", emailVerified);
        claims.put("firebase", Map.of("sign_in_provider", provider));

        Constructor<FirebaseToken> constructor = FirebaseToken.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        return constructor.newInstance(claims);
    }
}
