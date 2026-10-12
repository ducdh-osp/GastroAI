package vn.gastroai.be.infrastructure.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.patient.JournalTrashCleanupService;

/** UC0020 - Chay hang dem (mac dinh 3h45 UTC - lech voi job dong dep luc 3h15/3h30 dang
 * co) de xoa han cac muc nhat ky an uong/Bristol da qua han giu trong thung rac. */
@Component
public class JournalTrashPurgeScheduler {
    private final JournalTrashCleanupService service;

    public JournalTrashPurgeScheduler(JournalTrashCleanupService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.journal.trash-purge-cron:0 45 3 * * *}", zone = "UTC")
    public void purge() {
        service.purgeExpired();
    }
}