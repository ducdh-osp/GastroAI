package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC0005 - body của POST /auth/change-password (yêu cầu đã đăng nhập). */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword) {
}
