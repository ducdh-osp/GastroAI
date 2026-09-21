package vn.gastroai.be.api.auth;

import vn.gastroai.be.application.readmodel.AuthResult;

public record AuthResponse(String token, Long patientId, String email, String fullName) {
    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(result.token(), result.patientId(), result.email(), result.fullName());
    }
}
