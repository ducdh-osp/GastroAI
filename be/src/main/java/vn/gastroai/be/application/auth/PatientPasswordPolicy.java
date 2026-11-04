package vn.gastroai.be.application.auth;

import java.nio.charset.StandardCharsets;

/** Quy tắc độ mạnh áp dụng cho mật khẩu mới của tài khoản bệnh nhân. */
public final class PatientPasswordPolicy {

    public static final String REQUIREMENTS_MESSAGE =
            "Mật khẩu cần tối thiểu 8 ký tự, ít nhất 1 chữ in hoa, 1 chữ số và 1 ký tự đặc biệt";

    private PatientPasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(REQUIREMENTS_MESSAGE);
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu tối đa 72 ký tự");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu tối đa 72 byte UTF-8");
        }

        boolean hasUppercase = password.chars().anyMatch(character -> character >= 'A' && character <= 'Z');
        boolean hasDigit = password.chars().anyMatch(character -> character >= '0' && character <= '9');
        boolean hasSpecial = password.chars().anyMatch(character ->
                !((character >= 'A' && character <= 'Z')
                        || (character >= 'a' && character <= 'z')
                        || (character >= '0' && character <= '9')));
        if (!hasUppercase || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(REQUIREMENTS_MESSAGE);
        }
    }
}
