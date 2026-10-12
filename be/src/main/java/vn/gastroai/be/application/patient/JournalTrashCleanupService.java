package vn.gastroai.be.application.patient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** UC0020 - Duoc JournalTrashPurgeScheduler goi hang dem - xoa han (DELETE that) cac muc
 * nhat ky an uong/Bristol da nam trong thung rac qua han giu (mac dinh 30 ngay). */
@Service
public class JournalTrashCleanupService {

    private static final Logger log = LoggerFactory.getLogger(JournalTrashCleanupService.class);

    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final BristolLogRepository bristolLogRepository;
    private final int retentionDays;

    public JournalTrashCleanupService(FoodDiaryEntryRepository foodDiaryEntryRepository,
                                      BristolLogRepository bristolLogRepository,
                                      @Value("${app.journal.trash-retention-days:30}") int retentionDays) {
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.bristolLogRepository = bristolLogRepository;
        this.retentionDays = retentionDays;
    }

    @Transactional("postgresTransactionManager")
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int foodDiaryPurged = foodDiaryEntryRepository.purgeDeletedBefore(cutoff);
        int bristolPurged = bristolLogRepository.purgeDeletedBefore(cutoff);
        log.info("Da don thung rac: {} muc nhat ky an uong, {} ban ghi Bristol (cutoff={})",
                foodDiaryPurged, bristolPurged, cutoff);
    }
}