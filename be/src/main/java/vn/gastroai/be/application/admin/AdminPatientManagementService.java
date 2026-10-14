package vn.gastroai.be.application.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.user.CmsPatientResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.SearchQueryEscaper;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;

/** UC0046 - danh sach va khoa/mo tai khoan benh nhan. */
@Service
public class AdminPatientManagementService {
    private final PatientRepository patients;

    public AdminPatientManagementService(PatientRepository patients) {
        this.patients = patients;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public Page<CmsPatientResponse> list(String query, Boolean adminLocked, Pageable pageable) {
        String normalizedQuery = query == null || query.isBlank()
                ? null
                : SearchQueryEscaper.escapeLike(query.trim());
        return patients.searchForAdmin(normalizedQuery, adminLocked, pageable).map(CmsPatientResponse::from);
    }

    @Transactional("postgresTransactionManager")
    public CmsPatientResponse lock(Long patientId) {
        Patient patient = getForUpdate(patientId);
        if (!patient.isAdminLocked()) {
            patient.setAdminLocked(true);
            patient.setAdminLockedAt(Instant.now());
            // Thu hoi ngay cac JWT da phat, ke ca token chua co request tiep theo.
            patient.setTokenVersion(patient.getTokenVersion() + 1);
        }
        return CmsPatientResponse.from(patient);
    }

    @Transactional("postgresTransactionManager")
    public CmsPatientResponse unlock(Long patientId) {
        Patient patient = getForUpdate(patientId);
        patient.setAdminLocked(false);
        patient.setAdminLockedAt(null);
        // Khong ha tokenVersion: token da thu hoi khi khoa van phai vo hieu sau khi mo lai.
        return CmsPatientResponse.from(patient);
    }

    private Patient getForUpdate(Long patientId) {
        return patients.findByIdForUpdate(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan benh nhan"));
    }
}
