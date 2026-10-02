package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC0004 - body của POST /auth/forgot-password. */
public record ForgotPasswordRequest(
        @NotBlank @Email @Size(max = 255, message = "email toi da 255 ky tu") String email
) {
}
