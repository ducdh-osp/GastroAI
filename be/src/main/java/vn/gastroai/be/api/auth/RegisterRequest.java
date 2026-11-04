package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.gastroai.be.api.validation.MaxUtf8Bytes;

/** UC0001 - body của POST /auth/register. */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 255, message = "email toi da 255 ky tu") String email,
        @NotBlank
        @Size(min = 8, max = 72, message = "Mật khẩu cần tối thiểu 8 ký tự, ít nhất 1 chữ in hoa, 1 chữ số và 1 ký tự đặc biệt")
        @MaxUtf8Bytes(value = 72, message = "Mật khẩu tối đa 72 byte UTF-8") String password,
        @NotBlank @Size(max = 255) String fullName) {
}
