package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginSecurityPolicyTest {
    @Test
    void exposesConfiguredLockAndRetentionValues() {
        LoginSecurityPolicy policy = new LoginSecurityPolicy(5, 10, 90);

        assertEquals(5, policy.maxFailedAttempts());
        assertEquals(Duration.ofMinutes(10), policy.lockDuration());
        assertEquals(90, policy.historyRetentionDays());
    }
}
