package vn.gastroai.be.infrastructure.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSanitizerTest {

    @Test
    void truncateCutsLongValueAndReportsRemovedLength() {
        assertEquals("abc...(+7 ký tự)", LogSanitizer.truncate("abcdefghij", 3));
        assertEquals("ngan", LogSanitizer.truncate("ngan", 300));
        assertNull(LogSanitizer.truncate(null, 10));
    }

    @Test
    void maskEmailKeepsFirstTwoCharactersAndDomain() {
        assertEquals("th***@gmail.com", LogSanitizer.maskEmail("thang@gmail.com"));
        assertEquals("a***@x.vn", LogSanitizer.maskEmail("a@x.vn"));
        assertEquals("-", LogSanitizer.maskEmail(null));
        assertEquals("khong-phai-email", LogSanitizer.maskEmail("khong-phai-email"));
    }
}
