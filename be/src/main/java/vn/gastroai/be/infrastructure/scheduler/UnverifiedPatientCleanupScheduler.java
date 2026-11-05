package vn.gastroai.be.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.auth.UnverifiedPatientCleanupService;

/** Kiem tra moi phut de xoa tai khoan chua xac thuc co token da het han. */
@Component
public class UnverifiedPatientCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(UnverifiedPatientCleanupScheduler.class);

    private final UnverifiedPatientCleanupService service;

    public UnverifiedPatientCleanupScheduler(UnverifiedPatientCleanupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.security.unverified-account.cleanup-cron:0 * * * * *}", zone = "UTC")
    public void cleanup() {
        long deleted = service.cleanupExpiredAccounts();
        if (deleted > 0) {
            log.info("Da xoa {} tai khoan chua xac thuc co token het han", deleted);
        }
    }
}
