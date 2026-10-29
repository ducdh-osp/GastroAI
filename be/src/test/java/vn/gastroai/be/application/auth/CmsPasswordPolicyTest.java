package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CmsPasswordPolicyTest {

    private void assertRejected(String password, String expectedMessage) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CmsPasswordPolicy.validate(password));
        assertEquals(expectedMessage, error.getMessage());
    }

    @Test
    void strongPasswordIsAccepted() {
        assertDoesNotThrow(() -> CmsPasswordPolicy.validate("Gastro#Ai2026"));
    }

    @Test
    void rejectsTooShortOrTooLongOrNull() {
        assertRejected("Ab@1", "Mật khẩu mới phải dài từ 8 đến 72 ký tự");
        assertRejected("Aa@1".repeat(19), "Mật khẩu mới phải dài từ 8 đến 72 ký tự");
        assertRejected(null, "Mật khẩu mới phải dài từ 8 đến 72 ký tự");
    }

    @Test
    void rejectsMissingUppercase() {
        assertRejected("gastro#ai2026", "Mật khẩu mới phải có ít nhất 1 chữ hoa");
    }

    @Test
    void rejectsMissingLowercase() {
        assertRejected("GASTRO#AI2026", "Mật khẩu mới phải có ít nhất 1 chữ thường");
    }

    @Test
    void rejectsMissingDigit() {
        assertRejected("Gastro#AiGastro", "Mật khẩu mới phải có ít nhất 1 chữ số");
    }

    @Test
    void rejectsMissingSpecialCharacter() {
        assertRejected("GastroAi2026", "Mật khẩu mới phải có ít nhất 1 ký tự đặc biệt");
    }

    @Test
    void acceptsBoundaryLengths() {
        assertDoesNotThrow(() -> CmsPasswordPolicy.validate("Aa@12345"));
        assertDoesNotThrow(() -> CmsPasswordPolicy.validate("Aa@1".repeat(18)));
    }
}