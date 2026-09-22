package vn.gastroai.be.api.auth;

public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("Token dat lai mat khau khong hop le");
    }
}
