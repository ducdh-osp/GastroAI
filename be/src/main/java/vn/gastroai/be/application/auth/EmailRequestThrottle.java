package vn.gastroai.be.application.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/** Rate limit nho trong RAM, phu hop quyet dinh deploy mot backend instance. */
@Component
public class EmailRequestThrottle {

    public enum Action {
        RESEND_VERIFICATION,
        PASSWORD_RESET
    }

    private final Duration recipientCooldown;
    private final Duration ipWindow;
    private final int ipMaxRequests;
    private final Map<String, Instant> acceptedByRecipient = new HashMap<>();
    private final Map<String, ArrayDeque<Instant>> acceptedByIp = new HashMap<>();

    public EmailRequestThrottle(
            @Value("${app.mail.rate-limit.recipient-cooldown-seconds:60}") long recipientCooldownSeconds,
            @Value("${app.mail.rate-limit.ip-window-minutes:15}") long ipWindowMinutes,
            @Value("${app.mail.rate-limit.ip-max-requests:20}") int ipMaxRequests) {
        this.recipientCooldown = Duration.ofSeconds(Math.max(1, recipientCooldownSeconds));
        this.ipWindow = Duration.ofMinutes(Math.max(1, ipWindowMinutes));
        this.ipMaxRequests = Math.max(1, ipMaxRequests);
    }

    public synchronized boolean allow(Action action, String email, String ipAddress) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(ipWindow);
        cleanup(cutoff);

        String recipientKey = action + ":" + email.trim().toLowerCase(Locale.ROOT);
        String ipKey = action + ":" + (ipAddress == null ? "unknown" : ipAddress);
        Instant lastAccepted = acceptedByRecipient.get(recipientKey);
        if (lastAccepted != null && lastAccepted.plus(recipientCooldown).isAfter(now)) {
            return false;
        }

        ArrayDeque<Instant> ipRequests = acceptedByIp.computeIfAbsent(ipKey, ignored -> new ArrayDeque<>());
        while (!ipRequests.isEmpty() && ipRequests.peekFirst().isBefore(cutoff)) {
            ipRequests.removeFirst();
        }
        if (ipRequests.size() >= ipMaxRequests) {
            return false;
        }

        acceptedByRecipient.put(recipientKey, now);
        ipRequests.addLast(now);
        return true;
    }

    private void cleanup(Instant cutoff) {
        acceptedByRecipient.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        Iterator<Map.Entry<String, ArrayDeque<Instant>>> iterator = acceptedByIp.entrySet().iterator();
        while (iterator.hasNext()) {
            ArrayDeque<Instant> requests = iterator.next().getValue();
            while (!requests.isEmpty() && requests.peekFirst().isBefore(cutoff)) {
                requests.removeFirst();
            }
            if (requests.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
