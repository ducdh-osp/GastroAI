package vn.gastroai.be.domain.notification;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cac mau email he thong co dinh. Admin chi sua noi dung, khong tao ma mau tuy y. */
public enum EmailTemplateCode {
    ACCOUNT_VERIFICATION(
            "Xác thực tài khoản",
            Set.of("userName", "verificationUrl"),
            Set.of("verificationUrl"),
            Map.of(
                    "userName", "Nguyễn Văn A",
                    "verificationUrl", "https://gastroai.vn/verify-email?token=example")),
    PASSWORD_RESET(
            "Đặt lại mật khẩu",
            Set.of("userName", "resetUrl"),
            Set.of("resetUrl"),
            Map.of(
                    "userName", "Nguyễn Văn A",
                    "resetUrl", "https://gastroai.vn/reset-password?token=example")),
    MEDICATION_REMINDER_OFFLINE(
            "Nhắc uống thuốc khi offline",
            Set.of("applicationUrl"),
            Set.of("applicationUrl"),
            Map.of("applicationUrl", "https://gastroai.vn/medications"));

    private final String displayName;
    private final Set<String> allowedVariables;
    private final Set<String> requiredVariables;
    private final Map<String, String> sampleVariables;

    EmailTemplateCode(String displayName, Set<String> allowedVariables,
                      Set<String> requiredVariables, Map<String, String> sampleVariables) {
        this.displayName = displayName;
        this.allowedVariables = allowedVariables;
        this.requiredVariables = requiredVariables;
        this.sampleVariables = sampleVariables;
    }

    public String displayName() {
        return displayName;
    }

    public Set<String> allowedVariables() {
        return allowedVariables;
    }

    public Set<String> requiredVariables() {
        return requiredVariables;
    }

    public Map<String, String> sampleVariables() {
        return sampleVariables;
    }

    public List<String> sortedAllowedVariables() {
        return allowedVariables.stream().sorted().toList();
    }
}
