package ifsp.edu.projeto.cortaai.userservice.security.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class DataCrypto {

    private static final String PREFIX = "enc:v1:";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static volatile SecretKeySpec keySpec;

    private DataCrypto() {
    }

    public static void configure(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("CORTAAI_DATA_CRYPTO_KEY deve estar configurada para criptografar dados sensíveis.");
        }

        byte[] key = Base64.getDecoder().decode(base64Key.trim());
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException("CORTAAI_DATA_CRYPTO_KEY deve ser Base64 de uma chave AES de 16, 24 ou 32 bytes.");
        }
        keySpec = new SecretKeySpec(key, "AES");
    }

    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank() || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        ensureConfigured();

        try {
            byte[] iv = deterministicIv(plaintext);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao criptografar dado sensível.", ex);
        }
    }

    public static String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isBlank() || !storedValue.startsWith(PREFIX)) {
            return storedValue;
        }
        ensureConfigured();

        try {
            byte[] payload = Base64.getUrlDecoder().decode(storedValue.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Falha ao descriptografar dado sensível.", ex);
        }
    }

    public static String hmacSha256(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        ensureConfigured();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keySpec.getEncoded(), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao gerar hash de dado sensível.", ex);
        }
    }

    private static byte[] deterministicIv(String plaintext) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(keySpec.getEncoded(), HMAC_ALGORITHM));
        byte[] digest = mac.doFinal(("cortaai-data-v1:" + plaintext).getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(digest, IV_LENGTH_BYTES);
    }

    private static void ensureConfigured() {
        if (keySpec == null) {
            throw new IllegalStateException("Criptografia de dados sensíveis não foi inicializada.");
        }
    }
}
