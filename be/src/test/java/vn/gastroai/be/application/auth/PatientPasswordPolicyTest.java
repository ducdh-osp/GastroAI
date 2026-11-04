package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatientPasswordPolicyTest {

    @Test
    void acceptsPasswordMeetingAllRequirements() {
        assertDoesNotThrow(() -> PatientPasswordPolicy.validate("Password1!"));
    }

    @Test
    void explainsAllRequiredCharacterGroups() {
        for (String invalid : new String[]{"Short1!", "password1!", "Password!", "Password1"}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> PatientPasswordPolicy.validate(invalid));
            assertEquals(PatientPasswordPolicy.REQUIREMENTS_MESSAGE, error.getMessage());
        }
    }

    @Test
    void rejectsPasswordOverBcryptUtf8Limit() {
        String password = "A1!" + "ă".repeat(35);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> PatientPasswordPolicy.validate(password));
        assertEquals("Mật khẩu tối đa 72 byte UTF-8", error.getMessage());
    }
}
