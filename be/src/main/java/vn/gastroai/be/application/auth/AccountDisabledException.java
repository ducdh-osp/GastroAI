package vn.gastroai.be.application.auth;

/** Tai khoan bi khoa thu cong boi quan tri vien, khong co thoi diem tu mo. */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Tai khoan da bi khoa boi quan tri vien");
    }
}
