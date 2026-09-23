package vn.gastroai.be.application.auth;

import java.time.Instant;

/**
 * UC0008 - Ném ra khi tài khoản (Bệnh nhân hoặc Admin/Bác sĩ) đang trong thời gian bị khoá
 * tạm thời do sai mật khẩu quá nhiều lần. GlobalExceptionHandler map sang HTTP 423 Locked,
 * kèm lockedUntil để FE hiển thị còn khoá tới lúc nào.
 */
public class AccountLockedException extends RuntimeException {
    private final Instant lockedUntil;

    public AccountLockedException(Instant lockedUntil) {
        super("Tài khoản tạm thời bị khóa");
        this.lockedUntil = lockedUntil;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }
}
