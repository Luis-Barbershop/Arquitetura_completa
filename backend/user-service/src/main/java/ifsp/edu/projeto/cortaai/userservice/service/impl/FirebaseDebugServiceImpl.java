package ifsp.edu.projeto.cortaai.userservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInRequestDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseEmailSignInResponseDTO;
import ifsp.edu.projeto.cortaai.userservice.dto.FirebaseTokenDebugResponseDTO;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FirebaseDebugServiceImpl implements FirebaseDebugService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final FirebaseAuth firebaseAuth;
    private final ObjectMapper objectMapper;

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
}



