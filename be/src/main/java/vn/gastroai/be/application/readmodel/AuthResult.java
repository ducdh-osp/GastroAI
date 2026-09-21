package vn.gastroai.be.application.readmodel;

public record AuthResult(String token, Long patientId, String email, String fullName) {
}
