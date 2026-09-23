package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** UC0002 - body của POST /auth/login (Bệnh nhân). */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
