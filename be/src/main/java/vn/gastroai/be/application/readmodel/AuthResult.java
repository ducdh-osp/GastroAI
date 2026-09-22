package vn.gastroai.be.application.readmodel;

import java.time.Instant;

public record AuthResult(String token, Long patientId, String email, String fullName,
                         String role, Instant expiresAt) {
}
