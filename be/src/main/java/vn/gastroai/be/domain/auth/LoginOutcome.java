package vn.gastroai.be.domain.auth;

/**
 * Kết quả 1 lần thử đăng nhập, ghi vào *_login_history (dùng chung cho cả Bệnh nhân lẫn
 * Admin/Bác sĩ). BLOCKED khác FAILURE: BLOCKED là lần bị chặn vì tài khoản đang khoá hoặc
 * vừa chạm ngưỡng sai liên tiếp (UC0008) — FAILURE là các lần sai thông thường trước đó.
 */
public enum LoginOutcome {
    SUCCESS,
    FAILURE,
    BLOCKED
}
