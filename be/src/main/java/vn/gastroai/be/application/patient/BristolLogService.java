package vn.gastroai.be.application.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.patient.BristolListResponse;
import vn.gastroai.be.api.patient.BristolLogPoint;
import vn.gastroai.be.api.patient.BristolLogRequest;
import vn.gastroai.be.api.patient.BristolLogResponse;
import vn.gastroai.be.api.patient.BristolTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** UC0013 (ghi nhan Bristol) + UC0014 (xem xu huong theo thoi gian). */
@Service
public class BristolLogService {

    private final BristolLogRepository bristolLogRepository;
    private final PatientRepository patientRepository;

    public BristolLogService(BristolLogRepository bristolLogRepository, PatientRepository patientRepository) {
        this.bristolLogRepository = bristolLogRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional("postgresTransactionManager")
    public BristolLogResponse create(Long patientId, BristolLogRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
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
        bristolLogRepository.delete(loadOwned(patientId, logId));
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
        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);
        List<BristolLogPoint> points =
                bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(patientId, from, to).stream()
                        .map(log -> new BristolLogPoint(log.getLoggedAt(), log.getBristolType()))
                        .toList();
        return new BristolTrendResponse(points);
    }

    private BristolLog loadOwned(Long patientId, Long logId) {
        return bristolLogRepository.findById(logId)
                .filter(l -> l.getPatient().getId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("Bristol log not found or access denied"));
    }

    private BristolLogResponse toResponse(BristolLog log) {
        return new BristolLogResponse(log.getId(), log.getLoggedAt(), log.getBristolType(), log.getNotes());
    }
}
