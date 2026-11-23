package vn.gastroai.be.application.auth;

/**
 * Tai khoan bi khoa thu cong boi quan tri vien, khong co thoi diem tu mo. Message tieng Viet co
 * dau day du (giong quy uoc cua AccountLockedException) - hien thang ra FE (LoginPage.tsx), KHONG
 * qua BE_MESSAGE_MAP nhu cac loi dang nhap thong thuong.
 */
public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException() {
        super("Tài khoản đã bị tạm ngưng, vui lòng liên hệ hỗ trợ");
    }
}
