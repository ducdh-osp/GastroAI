package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC0004 - body của POST /auth/reset-password (bước 2, sau khi đã nhận token qua email). */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword
) {
}
