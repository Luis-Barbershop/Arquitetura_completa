package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import ifsp.edu.projeto.cortaai.userservice.dto.ChangePasswordRequestDTO;
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
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseAuthService;
import ifsp.edu.projeto.cortaai.userservice.service.FirebaseDebugService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class FirebaseDebugServiceImpl implements FirebaseDebugService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseDebugServiceImpl.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final FirebaseAuth firebaseAuth;
    private final FirebaseAuthService firebaseAuthService;
    private final ObjectMapper objectMapper;
    private final ifsp.edu.projeto.cortaai.userservice.repository.BarberRepository barberRepository;
    private final ifsp.edu.projeto.cortaai.userservice.repository.CustomerRepository customerRepository;

    @Value("${firebase.web-api-key:}")
    private String firebaseWebApiKey;

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

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
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
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
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

            HttpResponse<String> signUpResponse = HTTP_CLIENT.send(signUpRequest, HttpResponse.BodyHandlers.ofString());
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
                verifyPayload.put("continueUrl", "https://web.cortaai.shop/verify-email");
                String verifyBody = objectMapper.writeValueAsString(verifyPayload);
                HttpRequest verifyRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseWebApiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(verifyBody))
                        .build();
                HTTP_CLIENT.send(verifyRequest, HttpResponse.BodyHandlers.ofString());
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
        try {
            Map<String, Object> payload = Map.of(
                    "requestType", "PASSWORD_RESET",
                    "email", request.email()
            );
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + firebaseWebApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String code = root.path("error").path("message").asText("");
                String msg = switch (code) {
                    case "EMAIL_NOT_FOUND" -> "E-mail não encontrado.";
                    case "INVALID_EMAIL" -> "E-mail inválido.";
                    default -> "Erro Firebase: " + code;
                };
                throw new SecurityException(msg);
            }

            log.info("event=forgot-password-email-sent email={}", request.email());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requisição ao Firebase foi interrompida.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao comunicar com o Firebase: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void changePassword(ChangePasswordRequestDTO request) {
        if (firebaseWebApiKey == null || firebaseWebApiKey.isBlank()) {
            throw new IllegalArgumentException("A propriedade firebase.web-api-key não está configurada.");
        }
        try {
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

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
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

            log.info("event=change-password-ok uid={}", text(root, "localId"));

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requisição ao Firebase foi interrompida.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Falha ao comunicar com o Firebase: " + ex.getMessage(), ex);
        }
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
}



