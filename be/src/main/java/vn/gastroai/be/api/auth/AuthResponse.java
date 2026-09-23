package vn.gastroai.be.api.auth;

import vn.gastroai.be.application.readmodel.AuthResult;
import java.time.Instant;

/** Response trả về sau đăng nhập thành công — kèm JWT để FE lưu và gắn vào các request sau. */
public record AuthResponse(String token, Long patientId, String email, String fullName,
                           String role, Instant expiresAt) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.token(), result.patientId(), result.email(), result.fullName(),
                result.role(), result.expiresAt());
    }
}
