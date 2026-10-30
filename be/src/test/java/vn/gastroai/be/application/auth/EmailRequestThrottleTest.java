package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailRequestThrottleTest {

    @Test
    void appliesRecipientCooldownIndependentlyPerAction() {
        EmailRequestThrottle throttle = new EmailRequestThrottle(60, 15, 20);

        assertTrue(throttle.allow(EmailRequestThrottle.Action.PASSWORD_RESET,
                "User@Example.com", "127.0.0.1"));
        assertFalse(throttle.allow(EmailRequestThrottle.Action.PASSWORD_RESET,
                " user@example.com ", "127.0.0.2"));
        assertTrue(throttle.allow(EmailRequestThrottle.Action.RESEND_VERIFICATION,
                "user@example.com", "127.0.0.1"));
    }

    @Test
    void limitsRequestsFromSameIpAcrossDifferentRecipients() {
        EmailRequestThrottle throttle = new EmailRequestThrottle(60, 15, 2);

        assertTrue(throttle.allow(EmailRequestThrottle.Action.PASSWORD_RESET,
                "a@example.com", "203.0.113.1"));
        assertTrue(throttle.allow(EmailRequestThrottle.Action.PASSWORD_RESET,
                "b@example.com", "203.0.113.1"));
        assertFalse(throttle.allow(EmailRequestThrottle.Action.PASSWORD_RESET,
                "c@example.com", "203.0.113.1"));
    }
}
