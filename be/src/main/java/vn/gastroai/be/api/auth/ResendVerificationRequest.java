package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** UC0001 - body của POST /auth/resend-verification (issue #7 — cấp lại token đã hết hạn). */
public record ResendVerificationRequest(
        @NotBlank @Email String email) {
}
