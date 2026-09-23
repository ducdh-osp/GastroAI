package vn.gastroai.be.application.readmodel;

import java.time.Instant;

/** Kết quả login() ở tầng application — AuthResponse (tầng api) map lại từ record này. */
public record AuthResult(String token, Long patientId, String email, String fullName,
                         String role, Instant expiresAt) {
}
