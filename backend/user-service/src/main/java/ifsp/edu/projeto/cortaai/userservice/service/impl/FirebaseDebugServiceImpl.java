package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.AuthResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileBarberDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.CompleteProfileCustomerDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.EmailExistsResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseAuthRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailRegisterResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ForgotPasswordRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.ResendVerificationRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository;
import ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseDebugService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FirebaseDebugServiceImpl implements FirebaseDebugService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseDebugServiceImpl.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Duration FORGOT_PASSWORD_RESEND_COOLDOWN = Duration.ofMinutes(2);
    private static final Duration FORGOT_PASSWORD_LINK_TTL = Duration.ofHours(1);
    private static final ZoneId BRAZIL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final FirebaseAuth firebaseAuth;
    private final FirebaseAuthService firebaseAuthService;
    private final ObjectMapper objectMapper;
    private final BarberRepository barberRepository;
    private final CustomerRepository customerRepository;
    private final HttpClient httpClient;
    private final TokenVerifier tokenVerifier;

    @Autowired
    public FirebaseDebugServiceImpl(
            FirebaseAuth firebaseAuth,
            FirebaseAuthService firebaseAuthService,
            ObjectMapper objectMapper,
            BarberRepository barberRepository,
            CustomerRepository customerRepository
    ) {
        this(firebaseAuth, firebaseAuthService, objectMapper, barberRepository, customerRepository, HTTP_CLIENT);
    }

    FirebaseDebugServiceImpl(
            FirebaseAuth firebaseAuth,
            FirebaseAuthService firebaseAuthService,
            ObjectMapper objectMapper,
            BarberRepository barberRepository,
            CustomerRepository customerRepository,
            HttpClient httpClient
    ) {
        this(firebaseAuth, firebaseAuthService, objectMapper, barberRepository, customerRepository, httpClient,
                idToken -> firebaseAuth.verifyIdToken(idToken));
    }

    FirebaseDebugServiceImpl(
            FirebaseAuth firebaseAuth,
            FirebaseAuthService firebaseAuthService,
            ObjectMapper objectMapper,
            BarberRepository barberRepository,
            CustomerRepository customerRepository,
            HttpClient httpClient,
            TokenVerifier tokenVerifier
    ) {
        this.firebaseAuth = firebaseAuth;
        this.firebaseAuthService = firebaseAuthService;
        this.objectMapper = objectMapper;
        this.barberRepository = barberRepository;
        this.customerRepository = customerRepository;
        this.httpClient = httpClient;
        this.tokenVerifier = tokenVerifier;
    }

    @FunctionalInterface
    interface TokenVerifier {
        FirebaseToken verify(String idToken) throws FirebaseAuthException;
    }

    @Value("${firebase.web-api-key:}")
    private String firebaseWebApiKey;

    @Value("${app.web-base-url:https://web.cortaai.shop}")
    private String appWebBaseUrl;

    @Value("${app.forgot-password-continue-url:https://web.cortaai.shop/change-password}")
    private String forgotPasswordContinueUrl;

    private final Map<String, Instant> forgotPasswordLastRequestByEmail = new ConcurrentHashMap<>();

    @Override
    public FirebaseEmailSignInResponseDTO signInWithEmailPassword(FirebaseEmailSignInRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }

        try {
            Map<String, Object> payload = Map.of(
                    "email", request.email(),
                    "password", request.password(),
                    "returnSecureToken", true
            );

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                throw new SecurityException(extractFirebaseError(root));
            }

            return new FirebaseEmailSignInResponseDTO(
                    text(root, "idToken"),
                    text(root, "refreshToken"),
                    text(root, "expiresIn"),
                    text(root, "localId"),
                    text(root, "email"),
                    root.path("registered").asBoolean(false)
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requisicao ao Firebase foi interrompida.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao comunicar com o Firebase Authentication: " + ex.getMessage(), ex);
        }
    }

    @Override
    public FirebaseTokenDebugResponseDTO verifyToken(String idToken) {
        try {
            FirebaseToken token = tokenVerifier.verify(idToken);
            Map<String, Object> claims = token.getClaims();
            return new FirebaseTokenDebugResponseDTO(
                    token.getUid(),
                    token.getEmail(),
                    token.getName(),
                    token.getIssuer(),
                    text(claims.get("aud")),
                    toIso(claims.get("iat")),
                    toIso(claims.get("exp")),
                    claims
            );
        } catch (FirebaseAuthException e) {
            throw new SecurityException("Token Firebase inválido ou expirado: " + e.getMessage());
        }
    }

    @Override
    public FirebaseEmailRegisterResponseDTO registerWithEmailPassword(FirebaseEmailRegisterRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }

        String localId = null; // rastreia o UID criado para permitir rollback

        try {
            // ── 1. Criar usuário no Firebase via Identity Toolkit REST ─────────
            Map<String, Object> signUpPayload = Map.of(
                    "email", request.email(),
                    "password", request.password(),
                    "returnSecureToken", true
            );

            String signUpBody = objectMapper.writeValueAsString(signUpPayload);
            HttpRequest signUpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signUpBody))
                    .build();

            HttpResponse<String> signUpResponse = httpClient.send(signUpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode signUpRoot = objectMapper.readTree(signUpResponse.body());

            if (signUpResponse.statusCode() >= 400) {
                // Falha ANTES de criar o usuário — nenhum rollback necessário
                throw new SecurityException(extractRegisterError(signUpRoot));
            }

            String idToken      = text(signUpRoot, "idToken");
            localId             = text(signUpRoot, "localId"); // UID criado — salvo para rollback
            String refreshToken = text(signUpRoot, "refreshToken");
            String expiresIn    = text(signUpRoot, "expiresIn");
            String email        = request.email();

            log.info("event=firebase-signup-ok uid={} email={}", localId, email);

            // ── 2. Enviar e-mail de verificação (melhor-esforço, não bloqueia) ─
            try {
                // continueUrl: Firebase redireciona para esta URL após o clique no link,
                // com os parâmetros mode=verifyEmail&oobCode=XXXX&apiKey=...
                Map<String, Object> verifyPayload = new java.util.HashMap<>();
                verifyPayload.put("requestType", "VERIFY_EMAIL");
                verifyPayload.put("idToken", idToken);
                verifyPayload.put("continueUrl", appWebBaseUrl + "/verify-email");
                String verifyBody = objectMapper.writeValueAsString(verifyPayload);
                HttpRequest verifyRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseWebApiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(verifyBody))
                        .build();
                httpClient.send(verifyRequest, HttpResponse.BodyHandlers.ofString());
                log.info("event=verification-email-sent uid={}", localId);
            } catch (Exception e) {
                log.warn("event=verification-email-failed uid={} reason={}", localId, e.getMessage());
                // Não cancela o cadastro — e-mail pode ser reenviado depois
            }

            // ── 3. Provisionar no banco (verifyAndProvision) ──────────────────
            String userType = request.userType() == null ? "CUSTOMER" : request.userType().toUpperCase();
            firebaseAuthService.verifyAndProvision(new FirebaseAuthRequestDTO(idToken, userType));
            log.info("event=provision-ok uid={} userType={}", localId, userType);

            // ── 4. Completar perfil com email explícito (não há SecurityContext aqui) ─
            AuthResponseDTO profile;
            if ("BARBER".equals(userType)) {
                boolean isOwner = Boolean.TRUE.equals(request.isOwner());

                profile = firebaseAuthService.completeBarberProfile(localId,
                        new CompleteProfileBarberDTO(
                                request.tell(),
                                request.documentCPF(),
                                request.name(),
                                request.birthDate(),
                                isOwner
                        ),
                        email);
            } else {
                profile = firebaseAuthService.completeCustomerProfile(localId,
                        new CompleteProfileCustomerDTO(
                                request.tell(),
                                request.documentCPF(),
                                request.name(),
                                request.birthDate()
                        ),
                        email);
            }

            log.info("event=complete-profile-ok uid={}", localId);

            try {
                boolean isOwner = "BARBER".equals(userType) && Boolean.TRUE.equals(request.isOwner());
                firebaseAuthService.setCustomUserClaims(localId, userType, isOwner);
                log.info("event=custom-claims-updated uid={} role={} isOwner={}", localId, userType, isOwner);
            } catch (IllegalStateException ex) {
                log.warn("event=custom-claims-update-failed uid={} role={} reason={}", localId, userType, ex.getMessage());
            }

            return new FirebaseEmailRegisterResponseDTO(idToken, refreshToken, expiresIn, localId, profile);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            rollbackFirebaseUser(localId);
            throw new RuntimeException("Requisicao ao Firebase foi interrompida.", ex);
        } catch (IOException ex) {
            rollbackFirebaseUser(localId);
            throw new RuntimeException("Falha ao comunicar com o Firebase: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Falha nas etapas 3 ou 4: remove do Firebase para evitar estado inconsistente
            rollbackFirebaseUser(localId);
            throw ex;
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }

        sendForgotPasswordEmail(request.email(), false);
    }

    @Override
    public void resendForgotPassword(ForgotPasswordRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }

        sendForgotPasswordEmail(request.email(), true);
    }

    private void sendForgotPasswordEmail(String rawEmail, boolean isResend) {
        String normalizedEmail = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();

        if (isResend) {
            Instant lastRequest = forgotPasswordLastRequestByEmail.get(normalizedEmail);
            if (lastRequest != null) {
                Duration elapsed = Duration.between(lastRequest, now);
                if (elapsed.compareTo(FORGOT_PASSWORD_RESEND_COOLDOWN) < 0) {
                    long waitSeconds = FORGOT_PASSWORD_RESEND_COOLDOWN.minus(elapsed).getSeconds();
                    throw new IllegalArgumentException("Aguarde " + waitSeconds + " segundos antes de reenviar o link.");
                }
            }
        }

        ZonedDateTime requestTime = ZonedDateTime.ofInstant(now, BRAZIL_ZONE);
        ZonedDateTime expirationTime = requestTime.plus(FORGOT_PASSWORD_LINK_TTL);

        try {
            Map<String, Object> payload = Map.of(
                    "requestType", "PASSWORD_RESET",
                    "email", normalizedEmail,
                    "continueUrl", forgotPasswordContinueUrl
            );
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String code = root.path("error").path("message").asText("");
                String msg = switch (code) {
                    case "EMAIL_NOT_FOUND" -> "E-mail não encontrado.";
                    case "INVALID_EMAIL" -> "E-mail inválido.";
                    case "TOO_MANY_ATTEMPTS_TRY_LATER", "TOO_MANY_REQUESTS" -> "Muitas tentativas. Aguarde alguns minutos para tentar novamente.";
                    default -> "Erro Firebase: " + code;
                };
                throw new SecurityException(msg);
            }

            forgotPasswordLastRequestByEmail.put(normalizedEmail, now);
            log.info(
                    "event=forgot-password-email-sent email={} resend={} generatedAt={} estimatedExpiresAt={} continueUrl={}",
                    normalizedEmail,
                    isResend,
                    requestTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    expirationTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    forgotPasswordContinueUrl
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requisição ao Firebase foi interrompida.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao comunicar com o Firebase: " + ex.getMessage(), ex);
        }
    }

    @Override
    public ChangePasswordResponseDTO changePassword(ChangePasswordRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }
        try {
            FirebaseToken decodedToken = tokenVerifier.verify(request.idToken());
            String signInProvider = extractSignInProvider(decodedToken);
            if (signInProvider != null && !"password".equalsIgnoreCase(signInProvider)) {
                throw new IllegalArgumentException("Contas de login social nao podem alterar senha por esta rota.");
            }

            Map<String, Object> payload = Map.of(
                    "idToken", request.idToken(),
                    "password", request.newPassword(),
                    "returnSecureToken", true
            );
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:update?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String code = root.path("error").path("message").asText("");
                String msg = switch (code) {
                    case "INVALID_ID_TOKEN" -> "Token de sessão inválido ou expirado. Faça login novamente.";
                    case "WEAK_PASSWORD : Password should be at least 6 characters" -> "Senha muito curta (mínimo 6 caracteres).";
                    case "USER_NOT_FOUND" -> "Usuário não encontrado.";
                    case "TOKEN_EXPIRED" -> "Sessão expirada. Faça login novamente.";
                    default -> "Erro Firebase: " + code;
                };
                throw new SecurityException(msg);
            }

            String newIdToken = text(root, "idToken");
            String newRefreshToken = text(root, "refreshToken");
            log.info("event=change-password-ok uid={}", text(root, "localId"));
            return new ChangePasswordResponseDTO(newIdToken, newRefreshToken);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requisição ao Firebase foi interrompida.", ex);
        } catch (FirebaseAuthException ex) {
            throw new SecurityException("Token de sessao invalido ou expirado. Faca login novamente.");
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao comunicar com o Firebase: " + ex.getMessage(), ex);
        }
    }

    private String extractSignInProvider(FirebaseToken token) {
        if (token == null || token.getClaims() == null) {
            return null;
        }

        Object firebaseClaim = token.getClaims().get("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            return null;
        }

        Object provider = firebaseMap.get("sign_in_provider");
        if (provider == null) {
            return null;
        }
        return String.valueOf(provider).toLowerCase(Locale.ROOT);
    }

    /**
     * Rollback: remove o usuário do Firebase Auth se o cadastro falhou após o signUp.
     * Se uid for null (falha antes do signUp), não faz nada.
     */
    private void rollbackFirebaseUser(String uid) {
        if (uid == null || uid.isBlank()) return;
        try {
            firebaseAuth.deleteUser(uid);
            log.warn("event=firebase-rollback-ok uid={} — usuário removido do Firebase após falha no cadastro.", uid);
        } catch (FirebaseAuthException e) {
            log.error("event=firebase-rollback-failed uid={} reason={} — estado inconsistente! Requer limpeza manual.", uid, e.getMessage());
        }
    }

    private String extractRegisterError(JsonNode root) {
        String code = root.path("error").path("message").asText();
        if (code == null || code.isBlank()) {
            return "Falha ao cadastrar no Firebase.";
        }
        return switch (code) {
            case "EMAIL_EXISTS" -> "Este e-mail já está cadastrado no Firebase.";
            case "WEAK_PASSWORD : Password should be at least 6 characters" -> "Senha muito curta (mínimo 6 caracteres).";
            case "INVALID_EMAIL" -> "E-mail inválido.";
            case "OPERATION_NOT_ALLOWED" -> "Cadastro por e-mail/senha desabilitado no Firebase.";
            default -> "Erro Firebase: " + code;
        };
    }

    private String extractFirebaseError(JsonNode root) {
        String code = root.path("error").path("message").asText();
        if (code == null || code.isBlank()) {
            return "Falha ao autenticar no Firebase.";
        }
        return switch (code) {
            case "EMAIL_NOT_FOUND" -> "E-mail não encontrado no Firebase.";
            case "INVALID_PASSWORD" -> "Senha inválida no Firebase.";
            case "USER_DISABLED" -> "Usuário desativado no Firebase.";
            default -> "Erro Firebase: " + code;
        };
    }

    private String text(JsonNode root, String field) {
        return root.path(field).asText(null);
    }

    private String toIso(Object epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        if (epochSeconds instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue()).toString();
        }
        return String.valueOf(epochSeconds);
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public EmailExistsResponseDTO checkEmailExists(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail não pode ser vazio.");
        }
        String normalized = email.trim().toLowerCase();
        if (barberRepository.existsByEmailIgnoreCase(normalized)) {
            return new EmailExistsResponseDTO(true, "BARBER");
        }
        if (customerRepository.existsByEmailIgnoreCase(normalized)) {
            return new EmailExistsResponseDTO(true, "CUSTOMER");
        }
        return new EmailExistsResponseDTO(false, null);
    }

    @Override
    public void resendVerificationEmail(ResendVerificationRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }
        try {
            // 1. Sign-in silencioso para obter idToken fresco
            Map<String, Object> signInPayload = Map.of(
                    "email", request.email(),
                    "password", request.password(),
                    "returnSecureToken", true
            );
            String signInBody = objectMapper.writeValueAsString(signInPayload);
            HttpRequest signInRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(signInBody))
                    .build();
            HttpResponse<String> signInResponse = httpClient.send(signInRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode signInRoot = objectMapper.readTree(signInResponse.body());

            if (signInResponse.statusCode() >= 400) {
                String code = signInRoot.path("error").path("message").asText("");
                String msg = switch (code) {
                    case "EMAIL_NOT_FOUND" -> "E-mail não encontrado.";
                    case "INVALID_PASSWORD", "INVALID_LOGIN_CREDENTIALS" -> "Credenciais inválidas.";
                    case "USER_DISABLED" -> "Conta desativada.";
                    default -> "Erro ao autenticar: " + code;
                };
                throw new SecurityException(msg);
            }

            String idToken = signInRoot.path("idToken").asText(null);
            if (idToken == null) {
                throw new SecurityException("Não foi possível obter o token de autenticação.");
            }

            // 2. Enviar e-mail de verificação
            Map<String, Object> verifyPayload = new java.util.HashMap<>();
            verifyPayload.put("requestType", "VERIFY_EMAIL");
            verifyPayload.put("idToken", idToken);
            verifyPayload.put("continueUrl", "https://web.cortaai.shop/verify-email");
            String verifyBody = objectMapper.writeValueAsString(verifyPayload);
            HttpRequest verifyRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(verifyBody))
                    .build();
            HttpResponse<String> verifyResponse = httpClient.send(verifyRequest, HttpResponse.BodyHandlers.ofString());

            if (verifyResponse.statusCode() >= 400) {
                JsonNode verifyRoot = objectMapper.readTree(verifyResponse.body());
                String code = verifyRoot.path("error").path("message").asText("");
                throw new SecurityException("Erro ao reenviar verificação: " + code);
            }

            log.info("event=resend-verification-email-sent email={}", request.email());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requisição ao Firebase foi interrompida.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao comunicar com o Firebase: " + ex.getMessage(), ex);
        }
    }
}
