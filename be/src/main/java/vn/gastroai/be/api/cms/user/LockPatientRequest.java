package vn.gastroai.be.api.cms.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reason recorded in the existing audit log when an admin locks a patient account. */
public record LockPatientRequest(
        @NotBlank(message = "Lý do khóa tài khoản không được để trống")
        @Size(max = 1000, message = "Lý do khóa tài khoản tối đa 1000 ký tự") String reason
) {
    public LockPatientRequest {
        reason = reason == null ? null : reason.strip();
    }
}
