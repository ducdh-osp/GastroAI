package vn.gastroai.be.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import vn.gastroai.be.api.validation.MaxUtf8Bytes;

/** UC0005 - body của POST /auth/change-password (yêu cầu đã đăng nhập). */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 72, message = "mat khau toi da 72 ky tu")
        @MaxUtf8Bytes(value = 72, message = "mat khau khong duoc vuot qua 72 byte UTF-8") String currentPassword,
        @NotBlank
        @Size(min = 8, max = 72, message = "Mật khẩu cần tối thiểu 8 ký tự, ít nhất 1 chữ in hoa, 1 chữ số và 1 ký tự đặc biệt")
        @MaxUtf8Bytes(value = 72, message = "Mật khẩu tối đa 72 byte UTF-8") String newPassword) {
}
