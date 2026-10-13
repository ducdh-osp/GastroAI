package vn.gastroai.be.api.cms.user;

import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;

/** Thong tin toi thieu cua benh nhan cho man hinh quan tri. */
public record CmsPatientResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        boolean emailVerified,
        boolean adminLocked,
        Instant adminLockedAt,
        Instant lockedUntil,
        Instant createdAt
) {
    public static CmsPatientResponse from(Patient patient) {
        return new CmsPatientResponse(
                patient.getId(), patient.getEmail(), patient.getFullName(), patient.getPhone(),
                patient.isEmailVerified(), patient.isAdminLocked(), patient.getAdminLockedAt(),
                patient.getLockedUntil(), patient.getCreatedAt());
    }
}
