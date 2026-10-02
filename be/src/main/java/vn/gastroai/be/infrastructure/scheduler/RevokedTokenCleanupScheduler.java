package vn.gastroai.be.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.auth.RevokedTokenCleanupService;

/** Runs nightly so revoked_tokens does not grow without bound as patients log out. */
@Component
public class RevokedTokenCleanupScheduler {
    private final RevokedTokenCleanupService service;

    public RevokedTokenCleanupScheduler(RevokedTokenCleanupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.security.revoked-token.cleanup-cron:0 30 3 * * *}", zone = "UTC")
    public void cleanup() {
        service.cleanupExpiredTokens();
    }
}
