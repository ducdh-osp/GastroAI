package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.gastroai.be.api.validation.MaxUtf8Bytes;

/** UC0002 - body của POST /auth/login (Bệnh nhân). */
public record LoginRequest(
        @NotBlank @Email @Size(max = 255, message = "email toi da 255 ky tu") String email,
        @NotBlank @Size(max = 72, message = "mat khau toi da 72 ky tu")
        @MaxUtf8Bytes(value = 72, message = "mat khau khong duoc vuot qua 72 byte UTF-8") String password
) {
}
