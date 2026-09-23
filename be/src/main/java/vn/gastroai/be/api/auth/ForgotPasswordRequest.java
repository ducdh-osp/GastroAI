package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** UC0004 - body của POST /auth/forgot-password. */
public record ForgotPasswordRequest(
        @NotBlank @Email String email
) {
}
