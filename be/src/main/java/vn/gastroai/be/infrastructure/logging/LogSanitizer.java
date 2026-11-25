package vn.gastroai.be.infrastructure.logging;

/** Tiện ích làm sạch giá trị trước khi ghi log kỹ thuật — không để lộ dữ liệu cá nhân / log quá dài. */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /** Cắt chuỗi còn tối đa {@code max} ký tự, thêm "...(+N ký tự)" cho biết đã bỏ bao nhiêu. */
    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        int safeMax = Math.max(max, 0);
        return value.substring(0, safeMax) + "...(+" + (value.length() - safeMax) + " ký tự)";
    }

    /**
     * "thang@gmail.com" -> "th***@gmail.com". null -> "-"; chuỗi không phải email -> trả nguyên
     * (không đoán cách che 1 giá trị không rõ định dạng).
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return "-";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@') || at == email.length() - 1) {
            return email;
        }
        String local = email.substring(0, at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + email.substring(at);
    }
}
