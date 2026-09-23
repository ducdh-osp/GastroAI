package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.NotBlank;

/** UC0001 - body của POST /auth/verify-email (bấm link trong email xác thực). */
public record VerifyEmailRequest(@NotBlank String token) {
}
