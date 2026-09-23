package vn.gastroai.be.api.auth;

import vn.gastroai.be.application.readmodel.LoginHistoryItem;
import java.util.List;

/**
 * UC0007 - response phân trang cho GET /me/login-history (và tương đương bên CMS).
 * recentFailureCount tính trên TOÀN BỘ lịch sử (7 ngày gần nhất), không phải chỉ số dòng
 * "items" của trang đang xem — FE dùng field này để cảnh báo bảo mật thay vì tự đếm items,
 * tránh bỏ sót lần thất bại nằm ở trang khác.
 */
public record LoginHistoryResponse(
        List<LoginHistoryItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long recentFailureCount
) {
}
