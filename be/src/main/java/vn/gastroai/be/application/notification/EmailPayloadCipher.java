package vn.gastroai.be.application.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/** Ma hoa AES-GCM payload outbox, bao gom token mot lan va cac URL co token. */
@Component
public class EmailPayloadCipher {

    private static final String PREFIX = "v1.";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final ObjectMapper objectMapper;
    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailPayloadCipher(ObjectMapper objectMapper,
            @Value("${app.mail.payload-encryption-key}") String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() < 32) {
            throw new IllegalArgumentException("app.mail.payload-encryption-key phải có ít nhất 32 ký tự");
        }
        this.objectMapper = objectMapper;
        this.key = new SecretKeySpec(sha256(encryptionKey), "AES");
    }

    public String encrypt(Map<String, String> payload) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            secureRandom.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(objectMapper.writeValueAsBytes(payload));
            byte[] combined = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể mã hóa payload email", exception);
        }
    }

    public Map<String, String> decrypt(String encryptedPayload) {
        if (encryptedPayload == null || !encryptedPayload.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Định dạng payload email không hợp lệ");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedPayload.substring(PREFIX.length()));
            if (combined.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Payload email bị thiếu dữ liệu");
            }
            byte[] nonce = new byte[NONCE_LENGTH];
            byte[] encrypted = new byte[combined.length - NONCE_LENGTH];
            System.arraycopy(combined, 0, nonce, 0, NONCE_LENGTH);
            System.arraycopy(combined, NONCE_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] clear = cipher.doFinal(encrypted);
            return objectMapper.readValue(clear, new TypeReference<>() { });
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể giải mã payload email", exception);
        }
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo khóa mã hóa email", exception);
        }
    }
}
