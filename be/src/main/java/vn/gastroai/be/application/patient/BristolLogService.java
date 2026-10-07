package vn.gastroai.be.application.patient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.OwnedResourceLoader;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.api.patient.BristolListResponse;
import vn.gastroai.be.api.patient.BristolLogPoint;
import vn.gastroai.be.api.patient.BristolLogRequest;
import vn.gastroai.be.api.patient.BristolLogResponse;
import vn.gastroai.be.api.patient.BristolTrashItem;
import vn.gastroai.be.api.patient.BristolTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** UC0013 (ghi nhan Bristol) + UC0014 (xem xu huong theo thoi gian) + UC0020 (thung rac). */
@Service
public class BristolLogService {

    private final BristolLogRepository bristolLogRepository;
    private final PatientRepository patientRepository;
    // UC0020 - so ngay giu trong thung rac truoc khi bi don dep that su (xem application.yml).
    private final int retentionDays;

    public BristolLogService(BristolLogRepository bristolLogRepository,
                             PatientRepository patientRepository,
                             @Value("${app.journal.trash-retention-days:30}") int retentionDays) {
        this.bristolLogRepository = bristolLogRepository;
        this.patientRepository = patientRepository;
        this.retentionDays = retentionDays;
    }

    @Transactional("postgresTransactionManager")
    public BristolLogResponse create(Long patientId, BristolLogRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay benh nhan"));
        BristolLog log = new BristolLog(patient, request.loggedAt(), request.bristolType(), request.notes());
        return toResponse(bristolLogRepository.save(log));
    }

    @Transactional("postgresTransactionManager")
    public BristolLogResponse update(Long patientId, Long logId, BristolLogRequest request) {
        BristolLog log = loadOwned(patientId, logId);
        log.setLoggedAt(request.loggedAt());
        log.setBristolType(request.bristolType());
        log.setNotes(request.notes());
        return toResponse(bristolLogRepository.save(log));
    }

    @Transactional("postgresTransactionManager")
    public void delete(Long patientId, Long logId) {
        // loadOwned van dung findById nhu cu: dong da bi xoa truoc do se tu 404 vi
        // @SQLRestriction da an no khoi findById - dung y muon (khong xoa 2 lan).
        BristolLog log = loadOwned(patientId, logId);
        log.setDeletedAt(Instant.now());
        bristolLogRepository.save(log);
    }

    /** UC0020 - danh sach thung rac, con trong han 30 ngay (hoac so ngay cau hinh). */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<BristolTrashItem> listTrash(Long patientId) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        return bristolLogRepository.findTrash(patientId, cutoff).stream()
                .map(this::toTrashItem)
                .toList();
    }

    /** UC0020 - khoi phuc 1 ban ghi tu thung rac. 404 neu khong thay/khong phai cua benh nhan
     * nay, 409 neu da qua han giu (thuong la job don dep chua kip chay). */
    @Transactional("postgresTransactionManager")
    public BristolLogResponse restore(Long patientId, Long logId) {
        BristolLog log = bristolLogRepository.findDeletedOwned(logId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay muc ghi nhan Bristol trong thung rac hoac ban khong co quyen truy cap"));
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        if (log.getDeletedAt().isBefore(cutoff)) {
            throw new IllegalStateException("Da qua 30 ngay, khong the khoi phuc");
        }
        log.setDeletedAt(null);
        return toResponse(bristolLogRepository.save(log));
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public BristolListResponse list(Long patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BristolLog> result = bristolLogRepository.findByPatientIdOrderByLoggedAtDesc(patientId, pageable);
        List<BristolLogResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new BristolListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** UC0014 - moi lan ghi nhan la 1 diem tren bieu do (khong group/dem theo ngay nhu
     * food-diary, vi Bristol quan tam dung "gia tri tai thoi diem nao" chu khong phai
     * "tan suat/ngay"). */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public BristolTrendResponse trend(Long patientId, int days) {
        VietnamDateRange.Range range = VietnamDateRange.recentDaysIncludingToday(days);
        List<BristolLogPoint> points =
                bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                        patientId, range.fromInclusive(), range.toExclusive()).stream()
                        .map(log -> new BristolLogPoint(log.getLoggedAt(), log.getBristolType()))
                        .toList();
        return new BristolTrendResponse(points);
    }

    private BristolLog loadOwned(Long patientId, Long logId) {
        return OwnedResourceLoader.loadOwned(bristolLogRepository.findById(logId),
                l -> l.getPatient().getId().equals(patientId),
                "Khong tim thay muc ghi nhan Bristol hoac ban khong co quyen truy cap");
    }

    private BristolLogResponse toResponse(BristolLog log) {
        return new BristolLogResponse(log.getId(), log.getLoggedAt(), log.getBristolType(), log.getNotes());
    }

    private BristolTrashItem toTrashItem(BristolLog log) {
        Instant deletedAt = log.getDeletedAt();
        Instant purgeAt = deletedAt.plus(retentionDays, ChronoUnit.DAYS);
        return new BristolTrashItem(toResponse(log), deletedAt, purgeAt);
    }
}