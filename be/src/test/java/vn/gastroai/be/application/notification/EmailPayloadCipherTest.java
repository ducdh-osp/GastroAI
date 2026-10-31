package vn.gastroai.be.application.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailPayloadCipherTest {

    private final EmailPayloadCipher cipher = new EmailPayloadCipher(
            new ObjectMapper(), "test-email-encryption-key-at-least-32-characters");

    @Test
    void encryptsWithRandomNonceAndRoundTripsPayload() {
        Map<String, String> payload = Map.of("verificationUrl", "https://example.com?token=secret");

        String first = cipher.encrypt(payload);
        String second = cipher.encrypt(payload);

        assertNotEquals(first, second);
        assertFalse(first.contains("secret"));
        assertEquals(payload, cipher.decrypt(first));
        assertEquals(payload, cipher.decrypt(second));
    }

    @Test
    void rejectsTamperedCiphertextAndWeakKey() {
        String encrypted = cipher.encrypt(Map.of("resetUrl", "https://example.com"));
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";

        assertThrows(RuntimeException.class, () -> cipher.decrypt(tampered));
        assertThrows(IllegalArgumentException.class,
                () -> new EmailPayloadCipher(new ObjectMapper(), "too-short"));
    }
}
