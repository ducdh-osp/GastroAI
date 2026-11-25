package vn.gastroai.be.domain.auth;

/**
 * Nhãn thiết bị thân thiện ("Chrome on Windows") suy ra từ User-Agent, hiển thị ở lịch sử đăng nhập
 * (UC0007). Lịch sử đăng nhập KHÔNG lưu nhãn này thành cột riêng vì nó suy ra được từ user_agent
 * trong cùng dòng (3NF) - nơi nào cần thì gọi hàm này.
 */
public final class DeviceLabel {

    private DeviceLabel() {
    }

    public static String of(String userAgent) {
        if (userAgent == null) {
            userAgent = "unknown";
        }
        String browser = userAgent.contains("Edg/") ? "Edge"
                : userAgent.contains("Chrome/") ? "Chrome"
                : userAgent.contains("Firefox/") ? "Firefox"
                : userAgent.contains("Safari/") ? "Safari"
                : "Other browser";
        String operatingSystem = userAgent.contains("Windows") ? "Windows"
                : userAgent.contains("Mac OS") ? "macOS"
                : userAgent.contains("Android") ? "Android"
                : userAgent.contains("iPhone") ? "iPhone"
                : userAgent.contains("Linux") ? "Linux"
                : "Other device";
        return browser + " on " + operatingSystem;
    }
}
