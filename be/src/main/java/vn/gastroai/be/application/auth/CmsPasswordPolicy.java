package vn.gastroai.be.application.auth;

/** Quy tac do manh mat khau cho tai khoan CMS. */
public final class CmsPasswordPolicy {

    private CmsPasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("Mật khẩu mới phải dài từ 8 đến 72 ký tự");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 1 chữ hoa");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 1 chữ thường");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 1 chữ số");
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 1 ký tự đặc biệt");
        }
    }
}