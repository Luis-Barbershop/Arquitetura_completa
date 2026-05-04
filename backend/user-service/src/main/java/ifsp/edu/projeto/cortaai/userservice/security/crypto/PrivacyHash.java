package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import java.util.Locale;

public final class PrivacyHash {

    private PrivacyHash() {
    }

    public static String emailHash(String email) {
        String normalized = normalizeEmail(email);
        return normalized == null ? null : DataCrypto.hmacSha256(normalized);
    }

    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
