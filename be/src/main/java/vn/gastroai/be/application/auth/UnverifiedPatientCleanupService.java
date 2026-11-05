package vn.gastroai.be.application.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;

/** Xoa tai khoan chua xac thuc ngay sau khi token xac thuc het han. */
@Service
public class UnverifiedPatientCleanupService {
    private final PatientRepository patients;

    public UnverifiedPatientCleanupService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional("postgresTransactionManager")
    public long cleanupExpiredAccounts() {
        return patients.deleteExpiredUnverified(Instant.now());
    }
}
